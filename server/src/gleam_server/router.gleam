import gleam/http as http
import wisp
import wisp.{type Request, type Response}
import gleam_server/routes/widgets as widgets_routes

pub fn handle_request(request: Request) -> Response {
  case #(request.method, wisp.path_segments(request)) {
    #(http.Post, ["widgets", id, "analyze"]) ->
      widgets_routes.route_widgets_analyze(
        request,
        id
      )
    #(http.Post, ["widgets"]) ->
      widgets_routes.route_widgets_create(
        request
      )
    #(http.Delete, ["widgets", id]) ->
      widgets_routes.route_widgets_delete(
        request,
        id
      )
    #(http.Get, ["widgets"]) ->
      widgets_routes.route_widgets_list(
        request
      )
    #(http.Get, ["widgets", id]) ->
      widgets_routes.route_widgets_read(
        request,
        id
      )
    #(http.Patch, ["widgets", id]) ->
      widgets_routes.route_widgets_update(
        request,
        id
      )
    _ -> wisp.not_found()
  }
}
