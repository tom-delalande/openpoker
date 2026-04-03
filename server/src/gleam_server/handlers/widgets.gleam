import gleam_server/http as http_helpers
import gleam_server/models/widget as widget_model
import gleam_server/models/widget_merge_patch_update as widget_merge_patch_update_model
import wisp.{type Request, type Response}

pub fn widgets_analyze(request: Request, id: String) -> Response {
  let _ = request
  let _ = id
  http_helpers.not_implemented("widgets_analyze")
}

pub fn widgets_create(request: Request, body: widget_model.Widget) -> Response {
  let _ = request
  let _ = body
  http_helpers.not_implemented("widgets_create")
}

pub fn widgets_delete(request: Request, id: String) -> Response {
  let _ = request
  let _ = id
  http_helpers.not_implemented("widgets_delete")
}

pub fn widgets_list(request: Request) -> Response {
  let _ = request
  http_helpers.not_implemented("widgets_list")
}

pub fn widgets_read(request: Request, id: String) -> Response {
  let _ = request
  let _ = id
  http_helpers.not_implemented("widgets_read")
}

pub fn widgets_update(
  request: Request,
  id: String,
  body: widget_merge_patch_update_model.WidgetMergePatchUpdate,
) -> Response {
  let _ = request
  let _ = id
  let _ = body
  http_helpers.not_implemented("widgets_update")
}
