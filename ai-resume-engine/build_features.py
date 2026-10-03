"""
AI Resume Screening System - Top Level Feature Extraction & Dataset Splitting Entry Point
"""

import sys
import os

# Add src to python path
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "src"))

from features.build_features import main

if __name__ == "__main__":
    main()
