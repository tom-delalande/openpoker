import gleam_server/http as codec
import gleam/dynamic
import gleam/json

pub type WidgetMergePatchUpdate {
  WidgetMergePatchUpdate(
    id: Option(String),
    weight: Option(Int),
    color: Option(String)
  )
}

pub fn decoder() {
  dynamic.decode3(WidgetMergePatchUpdate,
    dynamic.optional_field(named: "id", of: dynamic.optional(of: dynamic.string)),
    dynamic.optional_field(named: "weight", of: dynamic.optional(of: dynamic.int)),
    dynamic.optional_field(named: "color", of: dynamic.optional(of: dynamic.string))
  )
}

pub fn to_json(model: WidgetMergePatchUpdate) -> json.Json {
  let WidgetMergePatchUpdate(
    id: id,
    weight: weight,
    color: color
  ) = model

  json.object([
    #("id", codec.option_json(id, fn(value) { json.string(value) })),
    #("weight", codec.option_json(weight, fn(value) { json.int(value) })),
    #("color", codec.option_json(color, fn(value) { json.string(value) }))
  ])
}
