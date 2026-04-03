# gleam_server

Generated Gleam server library core for a Wisp application.

## Generated layout

- `src/gleam_server/router.gleam`: top-level Wisp request dispatcher
- `src/gleam_server/routes/*.gleam`: route modules grouped by OpenAPI tag
- `src/gleam_server/handlers/*.gleam`: editable handler stubs
- `src/gleam_server/models/*.gleam`: generated request and response models

## Using the generated code

Import `gleam_server/router` into your existing Wisp application and call `handle_request/1` from your server entrypoint.

Handler modules return placeholder `501` responses by default and are intended to be edited by hand.
