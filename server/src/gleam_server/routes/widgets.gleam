import gleam/list
import gleam_server/handlers/widgets as handlers
import gleam_server/http as http_helpers
import gleam/http as http
import wisp
import wisp.{type Request, type Response}
import gleam_server/models/widget as widget_model
import gleam_server/models/widget_merge_patch_update as widget_merge_patch_update_model

pub fn route_widgets_analyze(request: Request, id: String) -> Response {
  handlers.widgets_analyze(
    request,
    id
  )
}

pub fn route_widgets_create(request: Request) -> Response {
  use raw_json <- wisp.require_json(request)
  case widget_model.decoder()(raw_json) {
    Ok(body) ->
      handlers.widgets_create(
        request,
        body,
      )
    Error(_) -> wisp.bad_request("Invalid JSON payload for widgets_create")
  }
}

pub fn route_widgets_delete(request: Request, id: String) -> Response {
  handlers.widgets_delete(
    request,
    id
  )
}

pub fn route_widgets_list(request: Request) -> Response {
  handlers.widgets_list(
    request
  )
}

pub fn route_widgets_read(request: Request, id: String) -> Response {
  handlers.widgets_read(
    request,
    id
  )
}

pub fn route_widgets_update(request: Request, id: String) -> Response {
  use raw_json <- wisp.require_json(request)
  case widget_merge_patch_update_model.decoder()(raw_json) {
    Ok(body) ->
      handlers.widgets_update(
        request,
        id,
        body,
      )
    Error(_) -> wisp.bad_request("Invalid JSON payload for widgets_update")
  }
}

