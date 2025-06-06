package com.example.pruebaapp

/**
 * A sealed hierarchy describing the state of the text generation.
 */
sealed interface UiState {

  /**
   * Empty state when the screen is first shown
   */
  object Initial : UiState

  /**
   * Still loading with tensorflow
   */
  object LoadingTensorFlow : UiState

  /**
   * scale numeric value between 0 and 1
   */
  data class SuccessTensorFlow(val outputText: String, val outputScale: Float) : UiState

  /**
   * Still loading with geminis
   */
  object LoadingGeminis : UiState
  data class SuccessGeminis(val outputText: String) : UiState

  /**
   * There was an error generating text
   */
  data class Error(val errorMessage: String) : UiState
}