return {
  name = "opa-authz",
  fields = {
    {
      config = {
        type = "record",
        fields = {
          { opa_url = { type = "string", required = true } },
          { introspection_url = { type = "string", required = true } },
          { introspection_client_id = { type = "string", required = true } },
          { introspection_client_secret = { type = "string", required = true } },
          { timeout_ms = { type = "number", default = 2000 } },
        },
      },
    },
  },
}
