import gleam_server/http as codec
import gleam/dynamic
import gleam/json

pub type Widget {
  Widget(
    id: String,
    weight: Int,
    color: String
  )
}

pub fn decoder() {
  dynamic.decode3(Widget,
    dynamic.field(named: "id", of: dynamic.string),
    dynamic.field(named: "weight", of: dynamic.int),
    dynamic.field(named: "color", of: dynamic.string)
  )
}

pub fn to_json(model: Widget) -> json.Json {
  let Widget(
    id: id,
    weight: weight,
    color: color
  ) = model

  json.object([
    #("id", json.string(id)),
    #("weight", json.int(weight)),
    #("color", json.string(color))
  ])
}
