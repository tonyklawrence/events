import Event.*


interface State<S, out A> {
    fun run(initial: S): Pair<S, A>
    fun <B> map(f: (A) -> B) = State<S, B> { s0 ->
        val (s, a) = run(s0)
        Pair(s ,f(a))
    }
    fun <B> flatMap(f: (A) -> State<S, B>) = State<S, B> { s0 ->
        val (s, a) = run(s0)
        f(a).run(s)
    }

    companion object {
        operator fun <S, A> invoke(f: (S) -> Pair<S, A>): State<S, A> = object: State<S, A> {
            override fun run(initial: S) = f(initial)
        }
    }
}

data class User(val id: Long, val fn: String, val ln: String)
class Cache

class Users {
    private fun check(id: Long): State<Cache, User?> = State { s -> Pair(s, null) }
    private fun retrieve(id: Long): State<Cache, User> = State { s -> Pair(s, User(id, "", "")) }

    fun findUser(id: Long): State<Cache, User> = check(id).flatMap { user: User? ->
        when (user) {
            is User -> State { c -> Pair(c, user) }
            else -> retrieve(id)
        }
    }
}

object Foo {
    init {
        val foo: State<Cache, User> = Users().findUser(100)
        val (cache, user) = foo.run(Cache())
    }
}

sealed class Event {
    data class Created(val width: Int, val height: Int) : Event()
    data class Plotted(val x: Int, val y: Int, val fill: Char) : Event()
    object Quit : Event()
}

class Canvas

fun applyEvent(e: Event) = State<Canvas, Event> { canvas ->
    when (e) {
        is Created -> Pair(canvas, e)
        is Plotted -> Pair(canvas, e)
        is Quit -> Pair(canvas, e)
    }
}

fun applyEvents(es: List<Event>) = State<Canvas, Unit> { canvas ->
    Pair(es.fold(canvas, { c, e -> applyEvent(e).run(c).first }), Unit)
}

object Commands {
    fun create(width: Int, height: Int) = applyEvent(Created(width, height))
    fun plot(x: Int, y: Int, fill: Char) = applyEvent(Plotted(x, y, fill))
    fun quit() = applyEvent(Quit)
}
