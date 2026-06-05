local cjson = require "cjson.safe"
local http = require "resty.http"

local OpaAuthz = {
  PRIORITY = 900,
  VERSION = "0.1.0",
}

local function decode_segment(segment)
  local remainder = #segment % 4
  if remainder > 0 then
    segment = segment .. string.rep("=", 4 - remainder)
  end

  segment = segment:gsub("-", "+"):gsub("_", "/")
  return ngx.decode_base64(segment)
end

local function decode_claims(token)
  local payload_segment = token:match("^[^.]+%.([^.]+)%.([^.]+)$")
  if not payload_segment then
    return nil
  end

  local decoded = decode_segment(payload_segment)
  if not decoded then
    return nil
  end

  return cjson.decode(decoded)
end

local function effective_role(claims)
  local realm_access = claims and claims.realm_access
  local roles = realm_access and realm_access.roles or {}

  for _, role in ipairs(roles) do
    if role == "ops-admin" then
      return "ops-admin"
    end
  end

  for _, role in ipairs(roles) do
    if role == "customer" then
      return "customer"
    end
  end

  return nil
end

local function claim_value(claim)
  if type(claim) == "table" then
    return claim[1]
  end

  return claim
end

local function claim_values(claim)
  if type(claim) == "table" then
    return claim
  end

  if claim == nil then
    return {}
  end

  return { claim }
end

local function introspect_token(conf, token)
  local httpc = http.new()
  httpc:set_timeout(conf.timeout_ms)

  local response, err = httpc:request_uri(conf.introspection_url, {
    method = "POST",
    body = ngx.encode_args({
      token = token,
    }),
    headers = {
      ["Authorization"] = "Basic " .. ngx.encode_base64(
        conf.introspection_client_id .. ":" .. conf.introspection_client_secret
      ),
      ["Content-Type"] = "application/x-www-form-urlencoded",
    },
  })

  if not response then
    return nil, err
  end

  if response.status ~= 200 then
    return nil, "introspection error"
  end

  return cjson.decode(response.body)
end

function OpaAuthz:access(conf)
  local auth_header = kong.request.get_header("authorization")
  if not auth_header then
    return kong.response.exit(401, { message = "missing bearer token" })
  end

  local token = auth_header:match("[Bb]earer%s+(.+)")
  if not token then
    return kong.response.exit(401, { message = "invalid bearer token" })
  end

  local introspection, introspection_err = introspect_token(conf, token)
  if not introspection then
    return kong.response.exit(503, { message = "introspection unavailable", detail = introspection_err })
  end

  if introspection.active ~= true then
    return kong.response.exit(401, { message = "inactive token" })
  end

  local claims = decode_claims(token)
  if not claims then
    return kong.response.exit(401, { message = "unreadable jwt payload" })
  end

  local account_id = kong.request.get_path():match("/api/accounts/([^/]+)")
  local request_body = cjson.encode({
    input = {
      method = kong.request.get_method(),
      path = kong.request.get_path(),
      account_id = account_id,
      customer_id = claim_value(claims.customer_id),
      account_ids = claim_values(claims.account_ids),
      role = effective_role(claims),
      username = claims.preferred_username,
    },
  })

  local httpc = http.new()
  httpc:set_timeout(conf.timeout_ms)

  local response, err = httpc:request_uri(conf.opa_url, {
    method = "POST",
    body = request_body,
    headers = {
      ["Content-Type"] = "application/json",
    },
  })

  if not response then
    return kong.response.exit(503, { message = "opa unavailable", detail = err })
  end

  local decision = cjson.decode(response.body)
  if response.status ~= 200 or not decision then
    return kong.response.exit(503, { message = "opa error" })
  end

  if decision.result ~= true then
    return kong.response.exit(403, { message = "forbidden" })
  end
end

return OpaAuthz
