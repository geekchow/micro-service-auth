# AGENTS.md

Repository notes for working in this workspace.

- CSDN zh series column: Microservice | 微服务(中文）
- CSDN index editor URL: https://editor.csdn.net/md/?articleId=162425519
- CSDN index article id: 162425519
- Common sub-article title prefix: 微服务认证与授权
- After each new sub-article publish, verify the index forward-link and update the index immediately if missing or outdated.
- Prefer minimal platform tags: one existing CSDN tag unless the user explicitly asks for more.
- Never use the CSDN tag `技术`; it is invalid in this workflow.
- Use `.csdn-tools/articles/_url_map.json` as the local source of truth for local Markdown file → published CSDN URL mappings.
- Refresh that map after every publish/update so frequent in-page link rewrites stay consistent.
- Treat `/memories/...` as internal memory storage, not a shell-visible filesystem path.
