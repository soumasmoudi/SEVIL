package tests;


import org.junit.After;

import org.junit.Test;
import entities.GroupManager;
import entities.Verifier;
import entities.Signer;
import entities.TrustedAuthority;
import it.unisa.dia.gas.jpbc.Element;
import statistics.base.Mean;
import statistics.base.Median;
import statistics.base.StandardDeviation;
import utilities.Message;
import utilities.MessageSigned;

public class TestsDePerformances {
	private String type = "a"; // pairing type (a for A-112 bits, a' for A-128 bits
	// f for F-112 bits and f' for F-128 bits

private int nb_execs = 10;// Number of iterations


// Lists of computation times of each algorithm
long[] time_set_params = new long[nb_execs];
long[] time_setup_SGr = new long[nb_execs];
long[] time_join_signer = new long[nb_execs];
long[] time_G_sign = new long[nb_execs];
long[] time_Batch_Verify = new long[nb_execs];
long[] time_Agg_Verify = new long[nb_execs];

long temps_total = 0;// Initialization of the test total computation time

long start;//
long end;

/** ----------------------------------------------------- */

@Test
public void testGlobal() {
System.out.println("Start test");
long start_temps_total = System.nanoTime();

	TrustedAuthority ta;
	GroupManager gm;
	Signer signer;
	Verifier ver;

	for (int i = 0; i < nb_execs; i++) {

		/**
		 * set_params
		 */
		start = System.nanoTime();
		ta = new TrustedAuthority(type);
		end = System.nanoTime();
		time_set_params[i] = (long) (end - start);


		/**
		 * setup_SGr
		 */

		start = System.nanoTime();
		gm = new GroupManager(ta); 
		end = System.nanoTime(); 
		time_setup_SGr[i] = (long) (end - start);

		/**
		 * join_SGr
		 */

		start = System.nanoTime(); 
		signer = new Signer(ta, gm); 
		end = System.nanoTime(); 
		time_join_signer[i] = (long) (end - start);


		/**
		 * G_sign
		 */
		
		Element M =ta.getDatabase().getParam().getG2().newRandomElement();
		start = System.nanoTime(); 
		signer.G_sign(M); 
		end = System.nanoTime(); 
		time_G_sign[i] = (long) (end - start);
		

		for (int j = 0; j < 100; j++) {
			Element msg =ta.getDatabase().getParam().getG2().newRandomElement();
			Message prf = signer.G_sign(msg); 
			end = System.nanoTime(); 
			signer.add_message(new MessageSigned(prf)); 
		}


		/**
		 * Batch_Verify
		 */
		ver = new Verifier(ta, gm.getDatabaseGM().getCrs());

		start = System.nanoTime();
		ver.Batch_Verify(signer.getMessage_list(), gm.getDatabaseGM().getCrs());
		end = System.nanoTime();
		time_Batch_Verify[i] = (long) (end - start);
		
		
		/**
		 * Agg_Verify
		 */
		start = System.nanoTime();
		ver.Agg_Verify(signer.getMessage_list().get(0), gm.getDatabaseGM().getCrs());
		end = System.nanoTime();
		time_Agg_Verify[i] = (long) (end - start);





		ta.executorShutdown();

	}

	long end_temps_total = System.nanoTime();
	temps_total = end_temps_total - start_temps_total;

	}

	@After
	public void after() {

		System.out.println("Tests performed with the pairing type " + type + " with a number of iterations of : " + nb_execs);


		System.out.println("set_params" + "\nmean in ms   " + String.valueOf(Mean.mean(time_set_params) / 1000000)
		+ "\nmedian in ms   " + String.valueOf(Median.median(time_set_params) / 1000000)
		+ "\nstandard deviation in ms   " + String.valueOf(StandardDeviation.standardDev(time_set_params) / 1000000));

		System.out.println("setup_SGr" + "\nmean in ms   " +
				String.valueOf(Mean.mean(time_setup_SGr) / 1000000) + "\nmedian in ms   "
				+ String.valueOf(Median.median(time_setup_SGr) / 1000000) +
				"\nstandard deviation in ms   " +
				String.valueOf(StandardDeviation.standardDev(time_setup_SGr) / 1000000));

		System.out.println("join_signer" + "\nmean in ms   " +
				String.valueOf(Mean.mean(time_join_signer) / 1000000) + "\nmedian in ms   " +
				String.valueOf(Median.median(time_join_signer) / 1000000) +
				"\nstandard deviation in ms   " +
				String.valueOf(StandardDeviation.standardDev(time_join_signer) / 1000000));



		System.out.println("G_sign" + "\nmean in ms   " +
		String.valueOf(Mean.mean(time_G_sign) / 1000000) + "\nmedian in ms   " +
		String.valueOf(Median.median(time_G_sign) / 1000000) +
		"\nstandard deviation in ms   " +
		String.valueOf(StandardDeviation.standardDev(time_G_sign) / 1000000));
		
		System.out.println("Batch_Verify" + "\nmean in ms   " +
		String.valueOf(Mean.mean(time_Batch_Verify) / 1000000) + "\nmedian in ms   "
		+ String.valueOf(Median.median(time_Batch_Verify) / 1000000) +
		"\nstandard deviation in ms   " +
		String.valueOf(StandardDeviation.standardDev(time_Batch_Verify) / 1000000));
		
		System.out.println("Agg_Verify" + "\nmean in ms   " +
		String.valueOf(Mean.mean(time_Agg_Verify) / 1000000) + "\nmedian in ms   "
		+ String.valueOf(Median.median(time_Agg_Verify) / 1000000) +
		"\nstandard deviation in ms   " +
		String.valueOf(StandardDeviation.standardDev(time_Agg_Verify) / 1000000));
		
		
		System.out.println("\nTotal computation time : " + String.valueOf(temps_total / 1000000000) + "s");
		
		System.out.println("\nTest performed on : " + java.util.Calendar.getInstance().getTime());
		System.out.println("\n-------------------------------------");
		System.out.println("\n-------------------------------------");
		
		
		}

}
