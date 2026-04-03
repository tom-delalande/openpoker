import gleam_server/http as codec
import gleam/dynamic
import gleam/json
import gleam_server/models/widget as widget_model

pub type WidgetList {
  WidgetList(
    items: List(widget_model.Widget)
  )
}

pub fn decoder() {
  dynamic.decode1(WidgetList,
    dynamic.field(named: "items", of: dynamic.list(of: widget_model.decoder()))
  )
}

pub fn to_json(model: WidgetList) -> json.Json {
  let WidgetList(
    items: items
  ) = model

  json.object([
    #("items", json.array(items, of: fn(entry) { widget_model.to_json(entry) }))
  ])
}
