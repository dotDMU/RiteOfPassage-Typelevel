package foundations

import cats.Functor

object Cats {

  /*
    type classes
    - Applicative
    - Functor
    - FlatMap
    - Monad
    - ApplicativeError/MonadError
   */


  /*
 * Functor-Implementierungen:
 *
 * - F[_] ist ein Typparameter höherer Ordnung(Higher kinded type), der einen Container-Typ darstellt
 * - Functor[F] ist eine Typklasse, die eine `map`-Operation für Typ F bereitstellt
 * - Der "using"-Parameter in Scala 3 ersetzt das frühere "implicit"
 * - increment: Nutzt direkt die functor.map-Methode zur Transformation
 * - increment_v2: Nutzt Cats' Syntax-Erweiterungen für elegantere Aufrufe
 * - Beide Funktionen erhöhen jeden Integer-Wert im Container um 1
 * - Generisch mit jedem Typ nutzbar, der Functor-Eigenschaften besitzt (List, Option, etc.)
 * - Demonstriert das Hauptkonzept von Functors: Transformation innerhalb eines Kontexts
 *   ohne den Kontext selbst zu verändern
 */

  // functor - "mappable" structures
  trait MyFunctor[F[_]] {
    def map[A, B](initialValue: F[A])(f: A => B): F[B]
  }

  // generalizable "mappable" APIs
  def increment[F[_]](container: F[Int])(using functor: Functor[F]): F[Int] =
    functor.map(container)(_ + 1)

  import cats.syntax.functor.*

  def increment_v2[F[_]](container: F[Int])(using functor: Functor[F]): F[Int] =
    container.map(_ + 1)

  // -----------------------------------------------------------------------------

  /*
   * Applicative-Implementierungen:
   *
   * - Applicative[F] erweitert Functor[F] und erbt die map-Fähigkeit
   * - Hauptfunktion von Applicative ist pure, die einen einfachen Wert in einen Kontext einbettet
   * - pure[A] erzeugt aus einem Wert vom Typ A einen Wert vom Typ F[A]
   * - Applicative ist die Grundlage für komplexere Operationen zwischen mehreren kontext behafteten Werten
   * - Im Beispiel wird der Integer-Wert 42 in eine Liste mit einem Element verpackt: List(42)
   * - Mit Applicative kann man Werte in beliebige Kontexte einbetten (List, Option, Either, etc.)
   * - Applicative ist ein fundamentaler Baustein für funktionale Programmierung mit "Effekten"
   *
   * Beispiele:
   * - Option als Kontext: 42.pure[Option] ergibt Some(42)
   * - Either als Kontext: "success".pure[Either[String, *]] ergibt Right("success")
   * - Mit unterschiedlichen Datentypen: "hello".pure[List] ergibt List("hello")
   */

  // applicative - pure, wrap existing values into "wrapper" values
  trait MyApplicative[F[_]] extends Functor[F] {
    def pure[A](value: A): F[A]
  }

  // Erste Implementierung - direkte Verwendung der Applicative-Typklasse

  import cats.Applicative

  val applicativeList = Applicative[List]
  val aSimpleList = applicativeList.pure(42)

  // Zweite Implementierung - mit Cats-Syntax-Erweiterungen

  import cats.syntax.applicative.*

  val aSimpleList_v2 = 42.pure[List]


  // -----------------------------------------------------------------

  /*
   * FlatMap-Implementierungen:
   *
   * - FlatMap[F] erweitert Functor[F] und erbt somit die map-Fähigkeit
   * - Die Hauptfunktion von FlatMap ist flatMap, die "verschachtelte Kontexte abflacht"
   * - flatMap nimmt einen Wert in einem Kontext F[A] und eine Funktion A => F[B] und liefert F[B]
   * - FlatMap erlaubt das Verketten von kontextbehafteten Operationen
   * - Im ersten Beispiel: List(1, 2, 3) wird zu List(1, 2, 2, 3, 3, 4) durch Abflachung
   *   - Für jedes Element x wird List(x, x+1) erzeugt und alle Ergebnislisten werden abgeflacht
   *
   * - crossProduct zeigt, wie FlatMap genutzt werden kann, um aus zwei Containern ein Kreuzprodukt zu erstellen
   * - crossProduct_v2 demonstriert die elegante for-comprehension Syntax, die intern auf flatMap und map abgebildet wird
   * - Die for-comprehension macht sequentielle Transformationen leichter lesbar
   *
   * Beispiel zum Verständnis:
   * - Bei List(1, 2) und List('a', 'b') erzeugt crossProduct List((1,'a'), (1,'b'), (2,'a'), (2,'b'))
   * - FlatMap ist besonders nützlich für "Effekte in Reihenfolge" - z.B. wenn der zweite Schritt
   *   vom Ergebnis des ersten Schritts abhängt
   * - Während Functor transformiert und Applicative einbettet, erlaubt FlatMap das Verketten und
   *   sequentielle Abarbeiten von Operationen in einem Kontext
   */

  trait MyFlatMap[F[_]] extends Functor[F] {
    def flatMap[A, B](initialValue: F[A])(f: A => F[B]): F[B]
  }

  // Direkte Verwendung der FlatMap-Typklasse
  import cats.FlatMap

  val flatMapList = FlatMap[List]
  val flatMappedList = flatMapList.flatMap(List(1, 2, 3))(x => List(x, x + 1))


  // Verwendung mit Cats' Syntax-Erweiterungen
  import cats.syntax.flatMap.*

  def crossProduct[F[_] : FlatMap, A, B](containerA: F[A], containerB: F[B]): F[(A, B)] =
    containerA.flatMap(a => containerB.map(b => (a, b)))

  // Verwendung mit for-comprehension Syntax
  def crossProduct_v2[F[_] : FlatMap, A, B](containerA: F[A], containerB: F[B]): F[(A, B)] =
    for {
      a <- containerA
      b <- containerB
    } yield (a,b)


  // ------------------------------------------------
  /*
   * Monad-Implementierungen:
   *
   * - Monad[F] vereint Applicative[F] und FlatMap[F] und kombiniert deren Fähigkeiten
   * - Durch diese Kombination erhält eine Monade sowohl:
   *   - Die Fähigkeit, Werte in einen Kontext zu einzubetten (pure von Applicative)
   *   - Die Fähigkeit, Operationen zu verketten (flatMap von FlatMap)
   *
   * - Die map-Implementierung wird in MyMonad überschrieben und nutzt flatMap und pure
   * - Diese Implementierung zeigt ein wichtiges Monad-Gesetz: map lässt sich durch flatMap und pure ausdrücken
   * - Für jeden Wert a wird die Funktion f angewendet und das Ergebnis mit pure eingebettet
   *
   * - Die crossProduct_v2-Funktion verwendet einen Monad-Kontextbound (F[_] : Monad)
   * - Die for-comprehension wird auf Monaden besonders mächtig, da sie sowohl map als auch flatMap nutzen kann
   * - Beachte: Der Code ist identisch zum vorherigen crossProduct_v2 mit FlatMap, funktioniert aber nun mit jedem Monad-Typ
   *
   * Bedeutung von Monaden in der funktionalen Programmierung:
   * - Monaden sind das grundlegende Baumuster für "sequentielle Berechnungen mit Effekten"
   * - Beispiele für monadische Berechnungen:
   *   - Option: Sequenz von Berechnungen, die alle erfolgreich sein müssen (sonst None)
   *   - List: Nicht-deterministische Berechnungen (mehrere mögliche Werte)
   *   - Either: Fehlerbehandlung in einer Kette von Operationen
   *   - Future: Asynchrone Berechnungen, die nacheinander ausgeführt werden müssen
   *
   * - Monaden erlauben, imperativ erscheinenden Code (for-comprehensions) zu schreiben,
   *   während sie die Effekte (Optionalität, Fehlerbehandlung, Asynchronität, etc.) abstrahieren
   */

  // Monad - applicative + flatMap
  trait MyMonad[F[_]] extends Applicative[F] with FlatMap[F] {
    override def map[A, B](fa: F[A])(f: A => B): F[B] =
      flatMap(fa)(a => pure(f(a)))
  }

  import cats.Monad
  val monadList = Monad[List]
  def crossProduct_v2[F[_] : Monad, A, B](containerA: F[A], containerB: F[B]): F[(A, B)] =
    for {
      a <- containerA
      b <- containerB
    } yield (a, b)


  // ------------------------------------------------------------------------------------
  // applicative-error - computations that can fail

  /*
   * ApplicativeError und MonadError - Fehlerbehaftete Berechnungen:
   *
   * - ApplicativeError[F, E] erweitert Applicative[F] um Fehlerbehandlung
   * - Der zusätzliche Typparameter E repräsentiert den Fehlertyp
   * - Hauptfunktion: raiseError[A](error: E): F[A] erzeugt einen fehlgeschlagenen Kontextwert
   *
   * - Die type-Alias ErrorOr[A] = Either[String, A] definiert einen konkreten Fehlerkontext
   *   - Left enthält einen Fehler vom Typ String
   *   - Right enthält einen erfolgreichen Wert vom Typ A
   *
   * - ApplicativeError erlaubt sowohl:
   *   - Erfolgreiche Berechnungen: applicativeEither.pure(42) erzeugt Right(42)
   *   - Fehlgeschlagene Berechnungen: applicativeEither.raiseError("Zonk") erzeugt Left("Zonk")
   *
   * - Die Syntax-Erweiterung ermöglicht eine intuitivere Schreibweise: "Zonk".raiseError
   *
   * - MonadError[F, E] erweitert sowohl ApplicativeError[F, E] als auch Monad[F]
   * - Dies kombiniert fehlerbehaftete Berechnungen mit monadischer Verkettung
   * - Mit MonadError können komplexe Fehlerbehandlungen in for-comprehensions verwendet werden
   *
   * Beispiele für typische Anwendungen:
   * - Option/Some/None: Einfache Fehlerbehandlung (Fehler ohne Details)
   * - Either/Right/Left: Fehlerbehandlung mit konkreten Fehlerbeschreibungen
   * - Future mit recover: Asynchrone Fehlerbehandlung
   * - IO-Monaden in FP-Bibliotheken: Fehlerbehaftete Effekte in reinen funktionalen Programmen
   *
   * Diese Typklassen ermöglichen es, Fehler als Werte zu behandeln und mit ihnen
   * in einer typensicheren, funktionalen Weise umzugehen, anstatt Exceptions zu werfen.
   */

  // Typklasse für Berechnungen, die fehlschlagen können
  trait MyApplicativeError[F[_], E] extends Applicative[F] {
    def raiseError[A](error: E): F[A]
  }

  // Beispiel mit Either als Fehlerkontext
  import cats.ApplicativeError
  type ErrorOr[A] = Either[String, A]
  val applicativeEither = ApplicativeError[ErrorOr, String]
  val desiredValue: ErrorOr[Int] = applicativeEither.pure(42) // Right(42)
  val failedValue: ErrorOr[Int] = applicativeEither.raiseError("Zonk") // Left("Zonk")

  // Mit Syntax-Erweiterungen
  import cats.syntax.applicativeError.*

  val failedValue_v2: ErrorOr[Int] = "Zonk".raiseError // Left("Zonk")

  // MonadError: Kombiniert ApplicativeError mit Monad
  trait MyMonadError[F[_], E] extends ApplicativeError[F, E] with Monad[F]

  import cats.MonadError

  val monadErrorEither = MonadError[ErrorOr, String]


  def main(args: Array[String]): Unit = {

  }
}
