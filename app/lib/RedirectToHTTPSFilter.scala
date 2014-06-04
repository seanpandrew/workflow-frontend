package lib

import java.net.{URLDecoder, URI}
import scala.concurrent.Future
import play.api.mvc.{Results, SimpleResult, RequestHeader, Filter}
import lib.syntax.RequestSyntax._


object RedirectToHTTPSFilter extends Filter {

  /** Uses the X-Forwarded-Proto header (added by Amazon's ELB) to determine whether
    * the client used HTTPS, and redirect if not.
    *
    * If the client did not come via an ELB, do nothing.
    *
    * Assumes untrusted clients can only connect via the ELB!
    */
  def apply(f: (RequestHeader) => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] =
    if (request.forwardedProtocol.exists(_ != "https")) {
      val queryString = Some(URLDecoder.decode(request.rawQueryString, "UTF-8")).filter(_.nonEmpty)
      val uri = new URI("https", request.host, request.path, queryString.orNull, null)
      Future.successful(Results.MovedPermanently(uri.toString))
    }
    else f(request)

}
