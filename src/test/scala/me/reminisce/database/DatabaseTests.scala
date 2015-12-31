package me.reminisce.database

import me.reminisce.fetcher.FetcherServiceSpec
import me.reminisce.service.ServerServiceActorSpec
import me.reminisce.service.gameboardgen.GameboardGeneratorSpec
import me.reminisce.service.gameboardgen.questiongen._
import me.reminisce.service.stats.StatsHandlerSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suites}

// This class will run all the database related tests so that the db can be closed after all are ran
// CAUTION : if you run tests individually the db will not be closed properly and the port won't be released
class DatabaseTests extends Suites(new OrderByPageLikesSpec, new OrderByPageLikeTimeSpec, new OrderByPostCommentsNumberSpec,
  new OrderByPostLikesNumberSpec, new OrderByPostTimeSpec, new WhenDidYouLikeThisPageSpec,
  new WhenDidYouShareThisPostSpec, new WhichCoordinatesWereYouAtSpec, new WhichPageDidYouLikeSpec, new WhoLikedYourPostSpec,
  new WhoMadeThisCommentOnYourPostSpec, new FetcherServiceSpec, new MongoDatabaseServiceSpec, new DeletionServiceSpec,
  new StatsHandlerSpec, new GameboardGeneratorSpec, new ServerServiceActorSpec)
with BeforeAndAfterAll with BeforeAndAfterEach {

  override def afterAll() = {
    DatabaseTestHelper.closeConnection()
  }

  override def beforeEach(): Unit = {
    Thread.sleep(200)
  }

}