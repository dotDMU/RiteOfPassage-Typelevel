package foundations

import cats.effect.kernel.{MonadCancelThrow, Resource}
import cats.effect.{IO, IOApp}
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor


object Doobie extends IOApp.Simple {
  case class Student(id: Int, name: String)

  private val xa: Transactor[IO] = Transactor.fromDriverManager[IO] (
    driver = "org.postgresql.Driver", // JDBC driver
    url = "jdbc:postgresql:demo", // database url
    user = "docker", // user name
    password = "docker", // password
    logHandler = None
  )

  private def findAllStudentNames: IO[List[String]] = {
    val query = sql"select name from students".query[String]
    val action = query.to[List]
    action.transact(xa)
  }

  private def saveStudent(id: Int, name: String): IO[Int] = {
    val query = sql"insert into students(id, name) values ($id, $name)"
    val action = query.update.run
    action.transact(xa)
  }

  private def findStudentsByInitial(letter: String): IO[List[Student]] = {
    val selectPart = fr"select id, name"
    val fromPart = fr"from students"
    val wherePart = fr"where left(name, 1) = $letter"

    val statement = selectPart ++ fromPart ++ wherePart
    val action = statement.query[Student].to[List]

    action.transact(xa)
  }

  // organize code
  trait Students[F[_]] { // "repo"
    def findById(id: Int): F[Option[Student]]
    def findAll: F[List[Student]]
    def create(name: String): F[Int]
  }

  object Students {
    def make[F[_]: MonadCancelThrow](xa: Transactor[F]): Students[F] = new Students[F] {
      def findById(id: Int): F[Option[Student]] = sql"select id, name from students where id=$id".query[Student].option.transact(xa)

      def findAll: F[List[Student]] = sql"select * from students".query[Student].to[List].transact(xa)

      def create(name: String): F[Int] = sql"insert into students(name) values ($name)".update.withUniqueGeneratedKeys[Int]("id").transact(xa)
    }
  }

  private val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](16)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver", // JDBC driver
      "jdbc:postgresql:demo", // database url
      "docker", // user name
      "docker", // password
      ec
    )
  } yield xa

  private val smallProgram: IO[Unit] = postgresResource.use { xa =>
    val studentsRepo = Students.make[IO](xa)
    for {
      id <- studentsRepo.create("Daniel")
      student <- studentsRepo.findById(id)
      _ <- IO.println(s"The first student of RokTheJvm is $student")

    } yield ()
  }

  override def run: IO[Unit] = {
//    findAllStudentNames.map(println)
//    saveStudent(3, "Alice").map(println)
//    findStudentsByInitial("m").map(println)
    smallProgram
  }
}
