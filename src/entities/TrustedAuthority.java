package entities;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import it.unisa.dia.gas.plaf.jpbc.pairing.a1.TypeA1CurveGenerator;
import it.unisa.dia.gas.plaf.jpbc.pairing.f.TypeFCurveGenerator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import databases.DatabaseTA;

public class TrustedAuthority {
	

	private final DatabaseTA database;
	private final PairingParameters pairingparameters;
	private final Pairing param;
	private final Element g2;
	private final Element g1;

	private final ExecutorService executor;

	public TrustedAuthority(String type) { // Represent the Set_params algorithm
		/**
		 * Type A 112 bits (rBits = 224; qBits = 1024) - 128 bits (rBits = 256; qBits =
		 * 1536)
		 */
		int rBits;
		int qBits;
		TypeACurveGenerator pga;
		TypeA1CurveGenerator pga1;

		switch (type) {
		case "a":
			rBits = 224;
			qBits = 1024;
			pga = new TypeACurveGenerator(rBits, qBits);
			pairingparameters = pga.generate();
			break;
		case "a_rapide"://Utilisé pour faire des calculs plus rapides lors de tests
			rBits = 40;
			qBits = 128;
			pga = new TypeACurveGenerator(rBits, qBits);
			pairingparameters = pga.generate();
			break;
		case "a'":
			rBits = 256;
			qBits = 1536;
			pga = new TypeACurveGenerator(rBits, qBits);
			pairingparameters = pga.generate();
			break;
		case "a1":
			pga1 = new TypeA1CurveGenerator(2, 512);
			pairingparameters = pga1.generate();
			break;
		case "f":
			pairingparameters = new TypeFCurveGenerator(224).generate();
			break;
		case "f'":
			pairingparameters = new TypeFCurveGenerator(256).generate();
			break;
		case "test":
			pairingparameters = new TypeFCurveGenerator(20).generate();
			break;
		default:
			System.out.println("Error type not supported");
			pairingparameters = null;
		}

		param = PairingFactory.getPairing(pairingparameters);
		g2 = param.getG2().newRandomElement();
		g1 = param.getG1().newRandomElement();


		executor = Executors.newCachedThreadPool(Executors.defaultThreadFactory());

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				executor.shutdown();
				while (true) {
					try {
						if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
							break;
						}
					} catch (InterruptedException e) {
					}
				}
			}
		}));

		database = new DatabaseTA(param, pairingparameters, g1, g2, executor);
	}


	public DatabaseTA getDatabase() {
		return this.database;
	}

	public ExecutorService getExecutor() {
		return executor;
	}

	public void executorShutdown() {// 
		executor.shutdown();
		while (true) {

			if (executor.isTerminated()) {
				break;
			}
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

}
