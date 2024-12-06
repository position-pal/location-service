package io.github.positionpal.location.storage

/** A trait that provides utility methods for storage operations. */
trait StorageUtils:
  import com.datastax.oss.driver.api.core.cql.SimpleStatement
  import scala.concurrent.Future
  import scala.jdk.CollectionConverters._
  import cats.effect.kernel.Async
  import cats.implicits.catsSyntaxMonadError

  /** Creates a new instance of a Cassandra CQL statement.
    * @param query the CQL query string
    * @param values the values to bind to the query
    * @return a new instance of [[SimpleStatement]]
    */
  def cql(query: String, values: Any*): SimpleStatement = SimpleStatement.newInstance(query, values*)

  /** Creates a new instance of a Cassandra batch statement.
    * @param statements the list of statements to include in the batch
    * @return a new instance of [[SimpleStatement]] representing a batch statement
    */
  def batch(statements: List[SimpleStatement]): SimpleStatement =
    cql(
      s"BEGIN BATCH ${statements.map(_.getQuery).mkString(" ")} APPLY BATCH",
      statements.flatMap(_.getPositionalValues.asScala)*,
    )

  /** Executes an operation with error handling.
    * @param operation the operation to execute which may throw an exception
    * @tparam F the [[Async]] effect type
    * @tparam T the return type of the operation
    * @return the result of the operation or a [[DatabaseError]] encapsulated in the `F` effect if an exception is thrown
    */
  def executeWithErrorHandling[F[_]: Async, T](operation: => Future[T]): F[T] =
    Async[F].fromFuture(Async[F].delay(operation)).adaptError { case e: Exception => DatabaseError(e.getMessage) }
