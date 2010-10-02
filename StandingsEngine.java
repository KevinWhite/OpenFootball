//import java.sql.*;
//
//public class StandingsEngine
//   {
//   private int coachNumber;
//   private int position;
//   private int wins;
//   private int losses;
//   private int draws;
//   private int scored;
//   private int conceded;
//   private int streakInt;
//   private int homeWon;
//   private int homeLost;
//   private int homeDrawn;
//   private int awayWon;
//   private int awayLost;
//   private int awayDrawn;
//   private int divisionWon;
//   private int divisionLost;
//   private int divisionDrawn;
//   private int conferenceWon;
//   private int conferenceLost;
//   private int conferenceDrawn;
//   private int interConferenceWon;
//   private int interConferenceLost;
//   private int interConferenceDrawn;
//   private int divisionNumber;
//   private boolean win = false;
//   private boolean loss = false;
//   private boolean draw = false;
//
//   StandingsEngine(int leagueNumber,
//                   int season,
//                   int week,
//                   int nextSeason,
//                   int nextWeek,
//                   Game game,
//                   boolean home,
//                   Connection database)
//      {
//      int teamNumber;
//      if (home == true)
//         {
//         teamNumber = game.getHomeTeamNumber();
//         }
//      else
//         {
//         teamNumber = game.getAwayTeamNumber();
//         }
//
//      try
//         {
//         Statement sql = database.createStatement();
//         ResultSet rs = sql.executeQuery("select * from Standings where LeagueNumber=" +
//                                         leagueNumber +
//                                         " and TeamNumber= " +
//                                         teamNumber +
//                                         " and Season=" +
//                                         season +
//                                         " and Week=" +
//                                         week +
//                                         " order by TeamNumber asc ");
//
//         rs.next();
//         coachNumber          = rs.getInt(6);
//         position             = rs.getInt(7);
//         wins                 = rs.getInt(8);
//         losses               = rs.getInt(9);
//         draws                = rs.getInt(10);
//         scored               = rs.getInt(11);
//         conceded             = rs.getInt(12);
//         streakInt            = rs.getInt(13);
//         homeWon              = rs.getInt(14);
//         homeLost             = rs.getInt(15);
//         homeDrawn            = rs.getInt(16);
//         awayWon              = rs.getInt(17);
//         awayLost             = rs.getInt(18);
//         awayDrawn            = rs.getInt(19);
//         divisionWon          = rs.getInt(20);
//         divisionLost         = rs.getInt(21);
//         divisionDrawn        = rs.getInt(22);
//         conferenceWon        = rs.getInt(23);
//         conferenceLost       = rs.getInt(24);
//         conferenceDrawn      = rs.getInt(25);
//         interConferenceWon   = rs.getInt(26);
//         interConferenceLost  = rs.getInt(27);
//         interConferenceDrawn = rs.getInt(28);
//         divisionNumber       = rs.getInt(29);
//         }
//      catch( SQLException error )
//         {
//         System.out.println("Database error in Team.Constructor(int,int,int,int,Connection) getting Standings: " + error.getMessage() );
//         }
//     if ((game.homeTeamWin() && home) ||
//         (game.awayTeamWin() && home == false))
//        {
//        wins++;
//        }
//     if ((game.awayTeamWin() && home) ||
//         (game.homeTeamWin() && home == false))
//        {
//        losses++;
//        }
////     if (game.draw())
////        {
////        draws++;
////        }
//     try
//         {
//         Statement sql = database.createStatement();
//         ResultSet rs = sql.executeQuery("INSERT INTO standings (" +
//                                         "LeagueNumber,Season,Week,TeamNumber,CoachNumber,Position" +
//                                         ",Wins,Losses,Draws,Scored,Conceded,Streak" +
//                                         ",DivisionWins,DivisionLosses,DivisionDraws" +
//                                         ",ConferenceWins,ConferenceLosses,ConferenceDraws" +
//                                         ",InterWins,InterLosses,InterDraws" +
//                                         ",HomeWins,HomeLosses,HomeDraws" +
//                                         ",AwayWins,AwayLosses,AwayDraws,DivisionNumber)" +
//                                         " VALUES (" +
//                                         leagueNumber +
//                                         "," +
//                                         nextSeason +
//                                         "," +
//                                         nextWeek +
//                                         "," +
//                                         teamNumber +
//                                         "," +
//                                         coachNumber +
//                                         "," +
//                                         position +
//                                         "," +
//                                         wins +
//                                         "," +
//                                         losses +
//                                         "," +
//                                         draws +
//                                         "," +
//                                         scored +
//                                          "," +
//                                         conceded +
//                                          "," +
//                                         streakInt +
//                                          "," +
//                                         divisionWon +
//                                         "," +
//                                         divisionLost +
//                                         "," +
//                                         divisionDrawn +
//                                         "," +
//                                         conferenceWon +
//                                         "," +
//                                         conferenceLost +
//                                         "," +
//                                         conferenceDrawn +
//                                         "," +
//                                         interConferenceWon +
//                                         "," +
//                                         interConferenceLost +
//                                         "," +
//                                         interConferenceDrawn +
//                                         "," +
//                                         homeWon +
//                                         "," +
//                                         homeLost +
//                                         "," +
//                                         homeDrawn +
//                                         "," +
//                                         awayWon +
//                                         "," +
//                                         awayLost +
//                                         "," +
//                                         awayDrawn +
//                                         "," +
//                                         divisionNumber +
//                                         ")");
//         }
//      catch( SQLException error )
//         {
//         System.out.println("Database error creating new standings: " + error.getMessage());
//         }
//      }
//
//      StandingsEngine(int leagueNumber,
//                      int season,
//                      int week,
//                      Connection database)
//      {
//      try
//         {
//         Statement sql = database.createStatement();
//         ResultSet rs = sql.executeQuery("select * from Standings where LeagueNumber=" +
//                                         leagueNumber +
//                                         " and Season=" +
//                                         season +
//                                         " and Week=" +
//                                         week +
//                                         " order by DivisionNumber asc, Wins desc, " +
//                                         " Draws desc, Scored desc, Conceded asc");
//         int position = 0;
//         int previousDivisionNumber = 0;
//         while(rs.next())
//            {
//            if (previousDivisionNumber != rs.getInt(29))
//               {
//               previousDivisionNumber = rs.getInt(29);
//               position = 0;
//               }
//            try
//              {
//              Statement sql2 = database.createStatement();
//              ResultSet rs2 = sql2.executeQuery("UPDATE standings " +
//                                                "SET Position = " +
//                                                position +
//                                                " WHERE StandingID = " +
//                                                rs.getInt(1));
//              }
//            catch( SQLException e )
//              {
//              System.out.println("Database error updating standings: " +e.getMessage() );
//              }
//            position++;
//            }
//         }
//      catch( SQLException error )
//         {
//         System.out.println("Database error retrieving standings: " + error.getMessage());
//         }
//      }
//   }