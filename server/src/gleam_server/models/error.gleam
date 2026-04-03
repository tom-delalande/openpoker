import gleam_server/http as codec
import gleam/dynamic
import gleam/json

pub type Error {
  Error(
    code: Int,
    message: String
  )
}

pub fn decoder() {
  dynamic.decode2(Error,
    dynamic.field(named: "code", of: dynamic.int),
    dynamic.field(named: "message", of: dynamic.string)
  )
}

pub fn to_json(model: Error) -> json.Json {
  let Error(
    code: code,
    message: message
  ) = model

  json.object([
    #("code", json.int(code)),
    #("message", json.string(message))
  ])
}
