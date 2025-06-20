# Functional Programming Basics: Effekt-Typen und Fehlerbehandlung

## Ziel dieses Dokuments

Dieses Dokument soll das Team dabei unterstützen, die wichtigsten FP-Konzepte im Kontext eures Play-Projekts zu verstehen und gezielt anzuwenden. Besonders im Fokus stehen Effekt-Typen (`Future`, `IO`, `F[_]`) und die Fehlerbehandlung mit `EitherT`.

---

## 1. Future vs IO – Was ist der Unterschied?

### Future\[A]

* **Was ist es?**
  Eine asynchrone Berechnung, die sofort nach ihrer Erstellung startet.
* **Beispiel:**

  ```scala
  val future: Future[Int] = Future { println("Boom"); 42 }
  ```

  → Die Berechnung beginnt **sofort**, auch wenn niemand sie abruft.

### IO\[A] (aus cats-effect)

* **Was ist es?**
  Eine Beschreibung einer Berechnung – **noch nicht gestartet**.
* **Beispiel:**

  ```scala
  val io: IO[Int] = IO { println("Boom"); 42 }
  ```

  → Nichts passiert, bis `.unsafeRunSync()` oder `.unsafeToFuture()` aufgerufen wird.

### Metapher:

| Typ    | Vergleich                                         |
| ------ | ------------------------------------------------- |
| Future | Essen wird sofort gekocht, egal ob du Hunger hast |
| IO     | Rezept, das du ausführen kannst, wenn du willst   |

---

## 2. Was ist `F[_]`?

* `F[_]` ist ein Platzhalter für einen Effekt-Typ.
* Es bedeutet: "Irgendein kontextueller Typ, der Berechnungen mit Nebenwirkungen beschreibt."
* Beispiele für `F[_]`:

  * `Future`
  * `IO`
  * `EitherT[IO, Error, *]`

### Warum benutzen?

* Um Funktionen **generisch** und **testbar** zu halten.
* Ermöglicht flexible Nutzung von IO, Future, etc. ohne Code duplizieren zu müssen.

### Beispiel:

```scala
trait JobRepository[F[_]] {
  def findById(id: UUID): F[Option[Job]]
}
```

---

## 3. Fehlerbehandlung mit EitherT

### Was ist `EitherT[F, E, A]`?

* Ein Wrapper um `F[Either[E, A]]`
* Erlaubt eine **monadische Fehlerbehandlung** in einem Effektkontext (Future, IO etc.)

### Vorteile:

* Elegantes Error-Short-Circuiting
* Klares Domain-Error-Modell (z. B. `sealed trait AppError`)

### Beispiel:

```scala
val result: EitherT[IO, AppError, Job] = for {
  input <- validate(inputData)
  job   <- repository.save(input)
} yield job
```

---

## 4. Logging mit Extension-Methoden

Wir verwenden z. B.:

```scala
extension [F[_], E, A](fa: F[A])
  def log(success: A => String, error: E => String): F[A] = ...
```

Damit können wir bei Bedarf Logging elegant einbauen:

```scala
service.createJob(jobInfo).log(
  success = job => s"Created job ${job.id}",
  error = err => s"Failed to create job: $err"
)
```

---

## 5. Wie wir damit arbeiten

* **Jetzt:**

  * Neue Features in strukturierter Architektur (Controller → Service → Repo)
  * `EitherT[Future, AppError, A]`
  * Logging mit Extension-Methoden

* **Nächste Schritte:**

  * Neue Services mit `IO` bauen
  * Controller können `.unsafeToFuture()` verwenden

* **Langfristig:**

  * IO als Standard-Effekt
  * Fehler zentral definieren
  * Play bleibt am Rand der Welt – der Kern wird testbar und funktional

---

## 6. Fragen, die du stellen kannst

* Muss das hier sofort starten, oder soll ich steuern, wann?
* Kann das fehlschlagen? Was mache ich mit dem Fehler?
* Ist dieser Code testbar ohne echten Seiteneffekt?
* Wollen wir Logging für Erfolg oder Fehler?

---

## Bei Fragen: Meldet euch :-)

Diese Themen sind für alle neu. Lieber gemeinsam durchdenken als blind refaktorisieren.


🔍 Warum zwei Extensions?
1. Future[A]

Fehler = Throwable im „äußeren Kontext“ → Logging via MonadError[Future, Throwable]

extension [F[_], A](fa: F[A])
  def logError(toMsg: Throwable => String)(using Logger[F], MonadError[F, Throwable]): F[A] =
    fa.attempt.flatTap {
      case Left(e)  => Logger[F].error(toMsg(e))
      case Right(_) => ().pure[F]
    }.rethrow

2. EitherT[F, AppError, A]

Fehler = AppError, nicht Throwable. Du brauchst leftSemiflatTap (nur für EitherT!).

import cats.data.EitherT
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

extension [F[_]: Monad, E, A](fa: EitherT[F, E, A])
  def logError(toMsg: E => String)(using Logger[F]): EitherT[F, E, A] =
    fa.leftSemiflatTap(e => Logger[F].error(toMsg(e)))

✅ Fazit
Typ	Fehler-Typ	Extension braucht
Future[A]	Throwable	MonadError[F, Throwable] + Logger[F]
EitherT[Future, E, A]	E (z. B. AppError)	Logger[F] (kein MonadError nötig)
👌 Vorteil:

Beide Extensions können parallel existieren, und dein Code nutzt immer die passende, ohne Konflikt:

val futureJob: Future[Job] = ...
val jobET: EitherT[Future, AppError, Job] = ...

futureJob.logError(t => s"Future failed: $t")
jobET.logError(e => s"Job creation failed: $e")

