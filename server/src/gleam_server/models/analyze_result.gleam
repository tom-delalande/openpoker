import gleam_server/http as codec
import gleam/dynamic
import gleam/json

pub type AnalyzeResult {
  AnalyzeResult(
    id: String,
    analysis: String
  )
}

pub fn decoder() {
  dynamic.decode2(AnalyzeResult,
    dynamic.field(named: "id", of: dynamic.string),
    dynamic.field(named: "analysis", of: dynamic.string)
  )
}

pub fn to_json(model: AnalyzeResult) -> json.Json {
  let AnalyzeResult(
    id: id,
    analysis: analysis
  ) = model

  json.object([
    #("id", json.string(id)),
    #("analysis", json.string(analysis))
  ])
}
