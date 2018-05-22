package eganjs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Configuration
class RoutingConfig {

    @Bean
    fun routes(userHandler: UserHandler) = router {
        "/api".nest {
            "/v1".nest {
                "/user".nest {
                    GET("/") {
                        userHandler.getAll()
                                .filter { it.group == Group.Gemini }
                                .collectList()
                                .flatMap { ServerResponse.ok().body(it.toFlux(), User::class.java) }
                    }
                    GET("/{name}") {
                        userHandler.getOne(it)
                                .filter { it.group == Group.Gemini }
                                .flatMap { ServerResponse.ok().body(it.toMono(), User::class.java) }
                                .switchIfEmpty(ServerResponse.notFound().build())
                    }
                }
                POST("/file")
            }
            "/v2".nest {
                "/user".nest {
                    GET("/") {
                        userHandler.getAll()
                                .collectList()
                                .flatMap { ServerResponse.ok().body(it.toFlux(), User::class.java) }
                    }
                    GET("/{name}") {
                        userHandler.getOne(it)
                                .flatMap { ServerResponse.ok().body(it.toMono(), User::class.java) }
                                .switchIfEmpty(ServerResponse.notFound().build())
                    }
                }
                POST("/upload")
            }
        }
    }
}

@Service
class UserHandler {

    private val users = listOf(
            User("Jim", Group.Caprica),
            User("Sam", Group.Caprica),
            User("Ben", Group.Gemini),
            User("Mel", Group.Caprica),
            User("Rey", Group.Gemini),
            User("Caz", Group.Gemini)
    )

    fun getAll(): Flux<User> = users.toFlux()

    fun getOne(request: ServerRequest): Mono<User> = Mono.fromCallable {
        val name = request.pathVariable("name")
        users.find { it.name.equals(name, ignoreCase = true) }
    }
}

enum class Group { Gemini, Caprica }

data class User(val name: String, val group: Group)
