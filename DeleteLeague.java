import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class DeleteLeague extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="DeleteLeague";

   public void init()
      {
      context = getServletContext();
      synchronized(context)
        {
        pool=(ConnectionPool)context.getAttribute("pool");
        if(pool==null)
          {
          String driverClassName = context.getInitParameter("driverClassName");
          String url = context.getInitParameter("url");
          String userName = context.getInitParameter("username");
          String password = context.getInitParameter("password");
          try
            {
            pool=new ConnectionPool(driverClassName,url,userName,password);
            }
          catch(Exception error)
            {
            Routines.writeToLog(servletName,"Unable to create connection pool : " + error,false,context);
            }
          context.setAttribute("pool",pool);
          }
        }
      }

   public void doGet(HttpServletRequest request,
                     HttpServletResponse response)
      {
      response.setContentType("text/html");
      PrintWriter webPageOutput=null;
      try
        {
        webPageOutput=response.getWriter();
        }
      catch(IOException error)
        {
        Routines.writeToLog(servletName,"Error getting writer : " + error,false,context);
        }
      HttpSession session=request.getSession();
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      if(Routines.loginCheck(true,request,response,database,context))
        {
        return;
        }
      int league=Routines.safeParseInt(request.getParameter("league"));
      try
        {
        Statement sql = database.createStatement();
        ResultSet queryResponse;
        queryResponse = sql.executeQuery("SELECT ConferenceNumber " +
                                         "FROM conferences " +
                                         "WHERE LeagueNumber = " +
                                         league);
        while(queryResponse.next())
           {
           int conferenceNumber = queryResponse.getInt(1);
           Statement sql2 = database.createStatement();
           ResultSet queryResponse2;
           queryResponse2 = sql2.executeQuery("Select DivisionNumber " +
                                              "FROM divisions " +
                                              "WHERE ConferenceNumber = " +
                                              conferenceNumber);
           while(queryResponse2.next())
              {
              int divisionNumber = queryResponse2.getInt(1);
              Statement sql3 = database.createStatement();
              ResultSet queryResponse3;
              queryResponse3 = sql3.executeQuery("SELECT TeamNumber " +
                                                 "FROM leagueteams " +
                                                 "WHERE DivisionNumber = " +
                                                 divisionNumber);
              while(queryResponse3.next())
                 {
                 int teamNumber = queryResponse3.getInt(1);
                 Statement sql4 = database.createStatement();
                 sql4.executeUpdate("DELETE FROM teams " +
                                    "WHERE TeamNumber = " +
                                    teamNumber);
                 sql4.executeUpdate("DELETE FROM coachteams " +
                                    "WHERE TeamNumber = " +
                                    teamNumber);
                 sql4.executeUpdate("DELETE FROM depthcharts " +
                                    "WHERE TeamNumber = " +
                                    teamNumber);
                 sql4.executeUpdate("DELETE FROM sets " +
                                    "WHERE TeamNumber = " +
                                    teamNumber);
                 sql4.executeUpdate("DELETE FROM situations " +
                                    "WHERE TeamNumber = " +
                                    teamNumber);
                 sql4.executeUpdate("DELETE FROM playbook " +
                                    "WHERE TeamNumber = " +
                                    teamNumber);
                 sql4.executeUpdate("DELETE FROM draftpriorities " +
                                    "WHERE TeamNumber = " +
                                    teamNumber);
				 sql4.executeUpdate("DELETE FROM gameboard " +
									"WHERE TeamNumber = " +
									teamNumber);                                    
                 }
              sql3.executeUpdate("DELETE FROM leagueteams " +
                                 "WHERE DivisionNumber = " +
                                 divisionNumber);
              }
           sql2.executeUpdate("DELETE FROM divisions " +
                              "WHERE ConferenceNumber = " +
                              conferenceNumber);
           }
  	    sql.executeUpdate("DELETE FROM draftratings " +
			   		      "WHERE LeagueNumber = " +
						  league);
        sql.executeUpdate("DELETE FROM conferences " +
                          " WHERE LeagueNumber = " +
                          league);
        sql.executeUpdate("DELETE FROM fixtures " +
                          " WHERE LeagueNumber = " +
                          league);
        sql.executeUpdate("DELETE FROM playbyplay " +
                          " WHERE League = " +
                          league);
        sql.executeUpdate("DELETE FROM players " +
                          " WHERE WorldNumber = " +
                          league);
        sql.executeUpdate("DELETE FROM standings " +
                          " WHERE LeagueNumber = " +
                          league);
        sql.executeUpdate("DELETE FROM draftboard " +
                          " WHERE LeagueNumber = " +
                          league);
        sql.executeUpdate("DELETE FROM draftboardteam " +
                          " WHERE LeagueNumber = " +
                          league);
        sql.executeUpdate("DELETE FROM leagues " +
                          " WHERE LeagueNumber = " +
                          league);
        }
      catch(SQLException error)
        {
        session.setAttribute("message",error.getMessage());
        try
          {
          response.sendRedirect(Routines.getRedirect(request,response,context));
          }
        catch(IOException error2)
          {
          Routines.writeToLog(servletName,"Error redirecting(1) : " + error2,false,context);
          }
        return;
        }
      finally
        {
        pool.returnConnection(database);
        }
      try
        {
        response.sendRedirect(Routines.encodeURL(request,response,"Main",null));
        }
      catch(IOException error2)
        {
        Routines.writeToLog(servletName,"Error redirecting(2) : " + error2,false,context);
        }
      return;
      }
   }