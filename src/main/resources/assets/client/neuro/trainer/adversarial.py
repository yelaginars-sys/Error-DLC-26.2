# -*- coding: utf-8 -*-
"""
Adversarial Training & Anticheat Check Simulation
Applies subtle perturbations during training to ensure smooth, natural mouse kinematics.
"""
import math
import numpy as np

def simulate_mouse_gcd(delta_angle, sensitivity=0.5):
    """Simulates Minecraft mouse sensitivity GCD quantization."""
    f = sensitivity * 0.6 + 0.2
    gcd = f * f * f * 8.0 * 0.15
    if gcd <= 0.0001:
        return delta_angle
    steps = round(delta_angle / gcd)
    return steps * gcd

def generate_adversarial_noise(batch_features, epsilon=0.01):
    """Adds small natural jitter perturbations to feature tensors."""
    noise = np.random.normal(0, epsilon, batch_features.shape).astype(np.float32)
    return batch_features + noise

def check_anticheat_heuristics(yaw_series, pitch_series):
    """
    Evaluates kinematic trajectory against common anticheat checks (Matrix, Grim, Vulcan).
    Returns a realism score [0.0, 1.0].
    """
    if len(yaw_series) < 3:
        return 1.0
    
    # Calculate angular acceleration
    dyaw = np.diff(yaw_series)
    dpitch = np.diff(pitch_series)
    
    ddyaw = np.diff(dyaw)
    ddpitch = np.diff(dpitch)
    
    # Severe jerk check
    max_jerk = np.max(np.abs(ddyaw)) if len(ddyaw) > 0 else 0
    if max_jerk > 60.0:
        return 0.2
        
    return 0.95
