import gleam/json
import gleam/http/request
import wisp
import wisp.{type Request, type Response}

pub fn query_params(request_: Request) -> List(#(String, String)) {
  case request.get_query(request_) {
    Ok(query) -> query
    Error(_) -> []
  }
}

pub fn option_json(value: Option(a), encoder: fn(a) -> json.Json) -> json.Json {
  case value {
    Some(inner) -> encoder(inner)
    None -> json.null()
  }
}

pub fn not_implemented(operation_name: String) -> Response {
  wisp.json_response(
    "{\"message\":\"TODO implement " <> operation_name <> "\"}",
    501,
  )
}
