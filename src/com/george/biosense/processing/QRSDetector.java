package com.george.biosense.processing;

import java.util.Arrays;

/**
 * @author Joro Class used for QRS detection, composed of a filtering stage and
 *         thresholding for QRS detection
 */
public class QRSDetector {
	private static int FILT_SIZE_LO = 30;
	private static int FILT_SIZE_HI = 30;
	private static int FILT_SIZE_DERIV = 3;
	private static int FILT_SIZE_AVG = 10;

	// Filter coefficients
	private static double[] lo_coeff = { 0.0569, -0.0376, -0.0352, -0.0357,
			-0.0359, -0.0333, -0.0261, -0.0136, 0.0044, 0.0269, 0.0521, 0.0777,
			0.1012, 0.1202, 0.1325, 0.1368, 0.1325, 0.1202, 0.1012, 0.0777,
			0.0521, 0.0269, 0.0044, -0.0136, -0.0261, -0.0333, -0.0359,
			-0.0357, -0.0352, -0.0376, 0.0569 }; // Low
	// pass
	// filter
	private static double[] hi_coeff = { -0.1251, -0.0141, -0.0148, -0.0155,
			-0.0161, -0.0167, -0.0173, -0.0179, -0.0184, -0.0187, -0.0191,
			-0.0194, -0.0196, -0.0197, -0.0198, 0.9801, -0.0198, -0.0197,
			-0.0196, -0.0194, -0.0191, -0.0187, -0.0184, -0.0179, -0.0173,
			-0.0167, -0.0161, -0.0155, -0.0148, -0.0141, -0.1251 }; // High
																	// Pass
																	// filter
	private static double[] deriv_coeff = { -1, 0, 1 }; // Differencing filter
	private static double avg_coeff = 0.125; // Average filter for past 40
												// samples

	// Buffers for filtering storage
	private double[] input_buff = new double[FILT_SIZE_LO];
	private double[] lo_buff = new double[FILT_SIZE_HI];
	private double[] hi_buff = new double[FILT_SIZE_DERIV];
	private double[] deriv_buff = new double[FILT_SIZE_AVG];

	// Output and input of filter
	double filtOut;
	double sampleIn;

	/**
	 * Initializing class used for ECG filtering and QRS detection
	 */
	public QRSDetector() {

		Arrays.fill(input_buff, 0); // Input samples (necessary for low pass
									// filtering)
		Arrays.fill(lo_buff, 0); // Output of low pass filter
		Arrays.fill(hi_buff, 0); // Output of hi pass filter
		Arrays.fill(deriv_buff, 0); // Output of derivative filter
		filtOut = 0; // Output of averaging filter
		sampleIn = 0;
	}

	/**
	 * @param sample
	 *            Sample value from file containing ECG
	 * @return Location of QRS if detected, or -1 otherwise
	 */
	public double isQRS(double sampleIn) {

		// Shifting arrays down by one to store new values from filtering
		// process
		System.arraycopy(input_buff, 0, input_buff, 1, input_buff.length - 1);
		System.arraycopy(lo_buff, 0, lo_buff, 1, lo_buff.length - 1);
		System.arraycopy(hi_buff, 0, hi_buff, 1, hi_buff.length - 1);
		System.arraycopy(deriv_buff, 0, deriv_buff, 1, deriv_buff.length - 1);
		input_buff[0] = sampleIn;

		filtOut = 0;
		/* FILTERING STAGE */
		for (int i = 0; i < FILT_SIZE_LO; i++) { // Low pass filter
			filtOut = filtOut + lo_coeff[i] * input_buff[FILT_SIZE_LO - 1 - i];
		}

		lo_buff[0] = filtOut;
		filtOut = 0;
		for (int i = 0; i < FILT_SIZE_HI; i++) { // High pass filter
			filtOut = filtOut + hi_coeff[i] * lo_buff[FILT_SIZE_HI - 1 - i];
		}

		hi_buff[0] = filtOut;
		filtOut = 0;
		for (int i = 0; i < FILT_SIZE_DERIV; i++) { // Derivative filter
			filtOut = filtOut + deriv_coeff[i]
					* hi_buff[FILT_SIZE_DERIV - 1 - i];
		}

		deriv_buff[0] = Math.abs(filtOut); // Absolute value
		filtOut = 0;
		for (int i = 0; i < FILT_SIZE_AVG; i++) { // AVG filter
			filtOut = filtOut + deriv_buff[FILT_SIZE_AVG - 1 - i] / avg_coeff;
		}

		return filtOut;

	}

}
