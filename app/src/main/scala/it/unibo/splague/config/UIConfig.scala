package it.unibo.splague.config

/** Centralized configuration for all UI-related constants. Contains window settings, GUI strings,
  * and validation error messages.
  */
object UIConfig:

  /** UI/Display configuration. Window dimensions and visual settings.
    */
  object Display:
    val WINDOW_WIDTH: Int = 800
    val WINDOW_HEIGHT: Int = 600
    val WINDOW_VISIBLE: Boolean = true

  /** GUI string constants. Application titles, labels, and button texts.
    */
  object Strings:
    val APP_TITLE: String = "SPlagueNet"
    val MENU_WELCOME: String = "Welcome to SPlagueNet"
    val GO_TO_SIMULATION: String = "Go to Simulation"
    val BACK_TO_MENU: String = "Back to menu"
    val EXIT_BUTTON: String = "Exit"

  /** Validation error messages. User-friendly error descriptions for invalid inputs.
    */
  object ErrorMessages:
    val AWARENESS_OUT_OF_BOUNDS: String = "Awareness must be in [0,1], got $value"
    val PROBABILITY_OUT_OF_BOUNDS: String = "Probability must be in [0,1], got $value"
    val EMPTY_SCENARIO_NAME: String = "The scenario name can't be empty"
    val NODE_NOT_IN_TOPOLOGY: String = "The starting node is not part of the topology"
    val INVALID_ITERATION_COUNT: String =
      "The number of maximum iterations must be positive and greater than 0"
    val NO_MALWARE_PROPAGATION_VECTORS: String =
      "Malware must declare at least one propagation vector"
    val EMPTY_NODE_ID: String = "The ID cannot be empty"
    val NODE_ID_WHITESPACE: String = "The ID cannot contain white space"
