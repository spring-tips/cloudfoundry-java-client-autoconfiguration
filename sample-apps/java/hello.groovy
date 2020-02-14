@RestController 
class GreetingsRestController {

    @GetMapping("/hello")
    String get (){
        return "Hello, world"
    }
}