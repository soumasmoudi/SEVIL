package tests; 

import entities.GroupManager;
import entities.Verifier;
import entities.Signer;

import entities.TrustedAuthority;
import it.unisa.dia.gas.jpbc.Element;
import statistics.base.*;
import utilities.Message;
import utilities.MessageSigned;

import org.junit.Test;


import static org.junit.Assert.assertTrue;

import org.junit.Before;

public class TestsDeFonctions {


	private int nb_iter; 

	private Verifier bv;
	private TrustedAuthority bta;
	private GroupManager bgm;
	private Signer bSigner;
	private String type;


	@Before
	public void before() {

		nb_iter = 100;

		type = "f";
		System.out.println("Calculs faits avec le type " + type);

		bta = new TrustedAuthority(type);
		bgm = new GroupManager(bta);
		bv = new Verifier(bta, bgm.getDatabaseGM().getCrs());

		bSigner = new Signer(bta, bgm);
	}

	@Test
	public void setparamsTest() {
		long[] time = new long[nb_iter];

		TrustedAuthority ta;

		for (int i = 0; i < nb_iter; i++) {
			long start = System.currentTimeMillis();
			ta = new TrustedAuthority(type);
			long end = System.currentTimeMillis();
			time[i] = (end - start);
		}
		double mean = Mean.mean(time);
		double sig = StandardDeviation.standardDev(time);
		System.out.println("setparams Test :");
		System.out.println("Number of iterations: " + nb_iter);
		System.out.println("Mean: " + mean + "\nStandard Deviation: " + sig + "\n");
	}


	@Test
	public void SetupSGrTest() {
		GroupManager gm;
		long[] time = new long[nb_iter];
		for (int i = 0; i < nb_iter; i++) {
			long start = System.currentTimeMillis();
			gm = new GroupManager(bta);
			long end = System.currentTimeMillis();
			time[i] = (long) (end - start);
		}
		double mean = Mean.mean(time);
		double sig = StandardDeviation.standardDev(time);
		System.out.println("Setup Group Test:");
		System.out.println("Number of iterations: " + nb_iter);
		System.out.println("Mean: " + mean + "\nStandard Deviation: " + sig + "\n");
	}

	@Test
	public void JoinSGrTest() {
		Signer signer;
		long[] time = new long[nb_iter];
		for (int i = 0; i < nb_iter; i++) {
			long start = System.currentTimeMillis();
			signer = new Signer(bta, bgm);
			long end = System.currentTimeMillis();
			time[i] = (long) (end - start);

		}
		double mean = Mean.mean(time);
		double sig = StandardDeviation.standardDev(time);
		System.out.println("Join Signer Test :");
		System.out.println("Number of iterations: " + nb_iter);
		System.out.println("Mean: " + mean + "\nStandard Deviation: " + sig + "\n");
	}



	@Test
	public void G_Sign_Test() {
		long[] time = new long[nb_iter];

		for (int i = 0; i < nb_iter; i++) {
			Element M =bta.getDatabase().getParam().getG2().newRandomElement();
			long start = System.currentTimeMillis();
			bSigner.G_sign(M);
			long end = System.currentTimeMillis();
			time[i] = (long) (end - start);
		}
		double mean = Mean.mean(time);
		double sig = StandardDeviation.standardDev(time);
		System.out.println("G_Sign Test:");
		System.out.println("Number of iterations: " + nb_iter);
		System.out.println("Mean: " + mean + "\nStandard Deviation: " + sig + "\n");
	}
	
	

	@Test
	public void Batch_Verify_Test() {
		long[] time = new long[nb_iter];
		boolean bool= false;;
		for (int i = 0; i < nb_iter; i++) {
			for (int j = 0; j < 100; j++) {
				Element msg =bta.getDatabase().getParam().getG2().newRandomElement();
				Message prf = bSigner.G_sign(msg); 
				bSigner.add_message(new MessageSigned(prf)); 
			}

			long start = System.currentTimeMillis();
			bool = bv.Batch_Verify(bSigner.getMessage_list(), bgm.getDatabaseGM().getCrs());
			long end = System.currentTimeMillis();
			time[i] = (long) (end - start);
		}
		assertTrue(bool);
		double mean = Mean.mean(time);
		double sig = StandardDeviation.standardDev(time);
		System.out.println("Batch_Verify Test:");
		System.out.println(bool);
		System.out.println("Number of iterations: " + nb_iter);
		System.out.println("Mean: " + mean + "\nStandard Deviation: " + sig + "\n");
	}

	
	
	@Test
	public void Agg_Verify_Test() {
		long[] time = new long[nb_iter];
		boolean bool= false;;
		for (int i = 0; i < nb_iter; i++) {
			
			Element msg =bta.getDatabase().getParam().getG2().newRandomElement();
			Message prf = bSigner.G_sign(msg); 
			bSigner.add_message(new MessageSigned(prf)); 
			

			long start = System.currentTimeMillis();
			bool = bv.Agg_Verify(bSigner.getMessage_list().get(0), bgm.getDatabaseGM().getCrs());
			long end = System.currentTimeMillis();
			time[i] = (long) (end - start);
		}
		assertTrue(bool);
		double mean = Mean.mean(time);
		double sig = StandardDeviation.standardDev(time);
		System.out.println("Agg_Verify Test:");
		System.out.println(bool);
		System.out.println("Number of iterations: " + nb_iter);
		System.out.println("Mean: " + mean + "\nStandard Deviation: " + sig + "\n");
	}


}
