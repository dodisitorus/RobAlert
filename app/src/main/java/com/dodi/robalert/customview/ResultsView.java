package com.dodi.robalert.customview;

import com.dodi.robalert.tflite.Classifier.Recognition;

import java.util.List;

public interface ResultsView {
  public void setResults(final List<Recognition> results);
}
