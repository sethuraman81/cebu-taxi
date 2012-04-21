package controllers;

import java.text.SimpleDateFormat;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;

import play.libs.Akka;
import play.mvc.Controller;
import play.mvc.Result;
import akka.actor.ActorRef;
import akka.actor.Props;
import async.LocationActor;
import async.LocationRecord;

public class Api extends Controller {
  
  private static MathTransform transform;
  private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - hh:mm:ss");
  
  public static MathTransform getTransform() {
    return transform;
  }

  public static SimpleDateFormat getSdf() {
    return sdf;
  }

  public Api() {
    System.setProperty("org.geotools.referencing.forceXY", "true");

    try {

      String googleWebMercatorCode = "EPSG:4326";
      
      String cartesianCode = "EPSG:4499";
       
      CRSAuthorityFactory crsAuthorityFactory = CRS.getAuthorityFactory(true);
       
      CoordinateReferenceSystem mapCRS = crsAuthorityFactory.createCoordinateReferenceSystem(googleWebMercatorCode);
       
      CoordinateReferenceSystem dataCRS = crsAuthorityFactory.createCoordinateReferenceSystem(cartesianCode);
                             
      boolean lenient = true; // allow for some error due to different datums
      transform = CRS.findMathTransform(mapCRS, dataCRS, lenient);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  public static Result location(String vehicleId, String timestamp, String latStr,
      String lonStr, String velocity, String heading, String accuracy) {
    
    final ActorRef locationActor = Akka.system().actorOf(
        new Props(LocationActor.class));
    
    try {
      
      final double lat = Double.parseDouble(latStr);
      final double lon = Double.parseDouble(lonStr);
      Coordinate obsCoords = new Coordinate(lon, lat);
      Coordinate obsPoint = new Coordinate();
      JTS.transform(obsCoords, obsPoint, transform);
      
      
      final LocationRecord location = new LocationRecord(vehicleId, sdf.parse(timestamp), 
          lat, lon, obsPoint.x, obsPoint.y,
          velocity != null ? Double.parseDouble(velocity) : null,
          heading != null ? Double.parseDouble(heading) : null,
          accuracy != null ? Double.parseDouble(accuracy) : null);

      locationActor.tell(location);

      return ok();
    } catch (final Exception e) {
      return badRequest();
    }
  }

}