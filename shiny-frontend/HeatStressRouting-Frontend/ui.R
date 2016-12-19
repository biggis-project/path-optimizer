library(shiny)
library(leaflet)
library(shinyBS)
library(shinyTime)

shinyUI(fluidPage(
  # include 'www/message-handler.js' and 'www/custom.css'
  singleton(tags$head(
    tags$script(src = "message-handler.js"),
    tags$head(tags$link(rel = "stylesheet", href = "custom.css"))
  )),
  navbarPage(
    "HeatStress",
    id = "navbar",
    ####################################################################
    # ROUTING:
    ####################################################################
    tabPanel("Routing",
             sidebarLayout(
               sidebarPanel(
                 bsAlert("routing_alerts"),
                 inlineBlock(
                   textInput("routing_start", "Start:", value = "49.0118083,8.4251357"),
                   width = "85%"
                 ),
                 # custom action button, see global.R
                 inlineActionButton("routing_select_start", "", icon = icon("map-marker")),
                 bsAlert("routing_alert_start"),
                 inlineBlock(
                   textInput("routing_destination", "Destination:", value = "49.0126868,8.4065707"),
                   width = "85%"
                 ),
                 inlineActionButton("routing_select_destination", "", icon = icon("map-marker")),
                 bsAlert("routing_alert_destination"),
                 inlineBlock(uiOutput("routing_date_input"), width = "63%"),
                 inlineBlock(uiOutput("routing_time_input"), class = "inline-time-input-container"),
                 bsAlert("routing_alert_datetime"),
                 checkboxGroupInput(
                   "routing_weightings",
                   "Weightings:",
                   choices = c("Heatindex" = "heatindex", "Temperature" = "temperature"),
                   selected = c("heatindex")
                 ),
                 checkboxGroupInput(
                   "routing_display_options",
                   "Display Options:",
                   choices =
                     if (isTRUE(enableRasterOverlay)) {
                       c("Show Bounding Box" = "show_bbox",
                         "Show Thermal Scan" = "show_thermalscan")
                     } else {
                       c("Show Bounding Box" = "show_bbox")
                     }
                 )
               ),
               mainPanel(
                 leafletOutput("map_routing", height = 400),
                 br(),
                 shiny::dataTableOutput("routing_results_table")
               )
             )),
    ####################################################################
    # OTPIMAL TIME:
    ####################################################################
    tabPanel("Optimal Time",
             sidebarLayout(
               sidebarPanel(
                 bsAlert("optimaltime_alerts"),
                 inlineBlock(
                   textInput("optimaltime_start", "Start:", value = "49.0118083,8.4251357"),
                   width = "85%"
                 ),
                 inlineActionButton("optimaltime_select_start", "", icon = icon("map-marker")),
                 bsAlert("optimaltime_alert_start"),
                 selectInput(
                   "optimaltime_place_type",
                   "Place Type:",
                   choices = c("supermarket", "bakery", "chemist", "pharmacy", "doctors")
                 ),
                 inlineBlock(uiOutput("optimaltime_date_input"), width = "63%"),
                 # inlineBlock(span(), width = "7%"), # TODO find a better solution
                 inlineBlock(uiOutput("optimaltime_time_input"), class = "inline-time-input-container"),
                 bsAlert("optimaltime_alert_datetime"),
                 sliderInput(
                   inputId = "optimaltime_radius",
                   label = "Radius (in meter):",
                   min = 0,
                   max = 5000,
                   value = 1000,
                   step = 10,
                   width = "96%"
                 ),
                 sliderInput(
                   inputId = "optimaltime_timebuffer",
                   label = "Time Buffer (in minutes):",
                   min = 0,
                   max = 120,
                   value = 15,
                   step = 1,
                   width = "96%"
                 ),
                 wellPanel(
                   class = "well-optional",
                   inlineBlock(
                     checkboxInput("optimaltime_earliest_enabled", "Earliest Time:"),
                     class = "inline-checkbox-container"
                   ),
                   inlineBlock(uiOutput("optimaltime_earliest_input"), class = "inline-time-input-container"),
                   br(),
                   inlineBlock(
                     checkboxInput("optimaltime_latest_enabled", "Latest Time:"),
                     class = "inline-checkbox-container"
                   ),
                   inlineBlock(uiOutput("optimaltime_latest_input"), class = "inline-time-input-container"),
                   bsAlert("optimaltime_alert_latest")
                 ),
                 radioButtons(
                   "optimaltime_rankby",
                   "Rank by:",
                   choices = c("value", "distance"),
                   selected = "value"
                 ),
                 checkboxGroupInput(
                   "optimaltime_display_options",
                   "Display Options:",
                   choices = 
                     if (isTRUE(enableRasterOverlay)) {
                       c(
                         "Show Search Radius" = "show_radius",
                         "Show Bounding Box" = "show_bbox",
                         "Show Thermal Scan" = "show_thermalscan"
                       )
                     } else {
                       c("Show Search Radius" = "show_radius",
                         "Show Bounding Box" = "show_bbox")
                     }
                   ,
                   selected = c("show_radius"
                   )
                 ),
                 sliderInput(
                   "optimaltime_max_results",
                   "Maximum Number of Resutls:",
                   value = 5,
                   min = 1,
                   max = 10,
                   step = 1,
                   width = '96%'
                 )
               ),
               mainPanel(
                 leafletOutput("map_optimaltime", height = 400),
                 br(),
                 shiny::dataTableOutput("optimaltime_results_table")
               )
             ))
  )
))
