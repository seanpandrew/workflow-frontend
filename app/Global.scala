import java.util.TimeZone
import play.api.mvc.WithFilters
import play.api.{Logger, GlobalSettings, Application}
import play.filters.gzip.GzipFilter
import lib.{RedirectToHTTPSFilter, LoggingFilter, LogConfig}
import config.Config

object Global extends WithFilters(RedirectToHTTPSFilter, new GzipFilter, LoggingFilter) with GlobalSettings {
  override def beforeStart(app: Application) {

    LogConfig.init()

    /* It's horrible, but this is absolutely necessary for correct interpretation
     * of datetime columns in prog almost certainly what no sane person ever wants to do.
     */

    System.setProperty("user.timezone", "UTC")
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }
}
