package it.unibo.splague.config

/** Centralized configuration for all simulation constants. Contains detection coefficients,
  * vulnerability factors, protocol parameters, and simulation timing.
  */
object SimulationConfig:

  /** Detection coefficients for each node type. Higher values indicate better anomaly detection
    * capabilities.
    */
  object Detection:
    val WORKSTATION_COEFFICIENT: Double = 1.0
    val SERVER_COEFFICIENT: Double = 1.5
    val ROUTER_COEFFICIENT: Double = 0.8
    val IOT_DEVICE_COEFFICIENT: Double = 0.3
    val MOBILE_DEVICE_COEFFICIENT: Double = 0.9

  /** Structural vulnerability coefficients for each node type. Higher values indicate more
    * susceptible to infection.
    */
  object Vulnerability:
    val WORKSTATION_FACTOR: Double = 1.0
    val SERVER_FACTOR: Double = 0.8
    val ROUTER_FACTOR: Double = 1.0
    val IOT_DEVICE_FACTOR: Double = 1.3
    val MOBILE_DEVICE_FACTOR: Double = 1.0

  /** Network protocol reliability parameters. Probability of successful message delivery.
    */
  object Protocols:
    val TCP_RELIABILITY: Double = 0.99
    val UDP_RELIABILITY: Double = 0.90

  /** Probability and awareness bounds. All probability-based values must fall within these ranges.
    */
  object Bounds:
    val MIN_PROBABILITY: Double = 0.0
    val MAX_PROBABILITY: Double = 1.0
    val MIN_AWARENESS: Double = 0.0
    val MAX_AWARENESS: Double = 1.0
    val DEFAULT_DETECTION_SIGNAL: Double = 0.0

  /** Detection formula components. Used in: Signal = workload ? detectionCoefficient ? (1 -
    * stealth)
    */
  object DetectionFormula:
    val STEALTH_INVERSION_FACTOR: Double = 1.0

  /** Infection formula components. Used in multi-step infection probability calculation.
    */
  object InfectionFormula:
    val DEFENSE_MODIFIER: Double = 1.0
    val PATCH_MODIFIER: Double = 1.0

  /** Simulation timing parameters. Controls the frequency of simulation steps and UI updates.
    */
  object Timing:
    val TIMER_INTERVAL_MS: Long = 200

  /** Malware behavior flags. Determines whether malware requires explicit trigger to propagate.
    */
  object MalwareBehavior:
    val WORM_AUTO_PROPAGATE: Boolean = false
    val VIRUS_REQUIRES_TRIGGER: Boolean = true
