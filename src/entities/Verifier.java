package entities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import databases.DatabaseTA;
import edu.jhu.isi.grothsahai.api.impl.VerifierImpl;
import edu.jhu.isi.grothsahai.entities.CommonReferenceString;
import edu.jhu.isi.grothsahai.entities.Matrix;
import edu.jhu.isi.grothsahai.entities.Proof;
import edu.jhu.isi.grothsahai.entities.QuarticElement;
import edu.jhu.isi.grothsahai.entities.SingleProof;
import edu.jhu.isi.grothsahai.entities.Statement;
import edu.jhu.isi.grothsahai.entities.Vector;
import edu.jhu.isi.grothsahai.enums.ProblemType;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import utilities.MessageSigned;
import utilities.VectorPPP;

public class Verifier {
	
	
	private final DatabaseTA databaseTA;// public parameters
	private final Element g2;
	private final Pairing param;
	private VerifierImpl verifier;
	private VectorPPP pppU;

	private final ExecutorService executor;

	public Verifier(TrustedAuthority ta, CommonReferenceString crs) {

		this.databaseTA = ta.getDatabase();
		this.g2 = databaseTA.getG2();
		this.param = databaseTA.getParam();
		this.verifier = new VerifierImpl(crs, ta.getExecutor());
		this.pppU = new VectorPPP(crs.getU1(), crs);

		this.executor = ta.getExecutor();

	}

    /** Batch_Verify Algorithm **/
	
	public boolean Batch_Verify(List<MessageSigned> messages, CommonReferenceString crs) {
		try {

			Element lhs_m = new QuarticElement(crs.getBT(), crs.getGT().newZeroElement(), crs.getGT().newZeroElement(),
					crs.getGT().newZeroElement(), crs.getGT().newZeroElement());
			Element rhs_m = new QuarticElement(crs.getBT(), crs.getGT().newZeroElement(), crs.getGT().newZeroElement(),
					crs.getGT().newZeroElement(), crs.getGT().newZeroElement());

			List<Future<Element>> ftr_lhs_m = new ArrayList<Future<Element>>();
			List<Future<Element>> ftr_rhs_m = new ArrayList<Future<Element>>();

			for (int i = 0; i < messages.size(); i++) {
				
				Proof proof = messages.get(i).getProofs()[0];
				Statement statement = messages.get(i).getStatements().get(0);
				SingleProof singleProof = proof.getProofs().get(0);
				Proof proof2 = messages.get(i).getProofs()[1];
				Statement statement2 = messages.get(i).getStatements().get(1);
				SingleProof singleProof2 = proof2.getProofs().get(0);


				ftr_rhs_m.add(executor.submit(new PairingVectorPPP(pppU, singleProof.getPi(), crs)));

			ftr_rhs_m.add(executor.submit(new PairingVector(singleProof.getTheta(), crs.getU2(), crs))); 


				ftr_rhs_m.add(executor.submit(new PairingVectorPPP(pppU, singleProof2.getPi(), crs)));

			ftr_rhs_m.add(executor.submit(new PairingVector(singleProof2.getTheta(), crs.getU2(), crs)));

				ftr_lhs_m.add(executor
						.submit(new PairingVector(proof.getC(), statement.getGamma().multiply(proof.getD()), crs)));
				ftr_lhs_m.add(executor
						.submit(new PairingVector(proof2.getC(), statement2.getGamma().multiply(proof2.getD()), crs)));

			}

			for (int i = 0; i < ftr_lhs_m.size(); i++) {
				try {
					lhs_m = lhs_m.add(ftr_lhs_m.get(i).get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
			for (int i = 0; i < ftr_rhs_m.size(); i++) {
				try {
					rhs_m = rhs_m.add(ftr_rhs_m.get(i).get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

			Element lhs_p = new QuarticElement(crs.getBT(), crs.getGT().newZeroElement(), crs.getGT().newZeroElement(),
					crs.getGT().newZeroElement(), crs.getGT().newZeroElement());
			Element rhs_p = new QuarticElement(crs.getBT(), crs.getGT().newZeroElement(), crs.getGT().newZeroElement(),
					crs.getGT().newZeroElement(), crs.getGT().newZeroElement());

			Vector a;
			Vector b;
			Vector c;
			Vector d;
			Vector C_sum;
			Vector D_sum;
			Matrix gamma;

			List<Future<Element>> ftr_lhs_p = new ArrayList<Future<Element>>();
			List<Future<Element>> ftr_rhs_p = new ArrayList<Future<Element>>();

			for (int j = 2; j < 6; j++) {
				c = messages.get(0).getProofs()[j].getC();
				d = messages.get(0).getProofs()[j].getD();
				C_sum = Vector.getQuadraticZeroVector(c.get(0).getField(), param, c.getLength());
				D_sum = Vector.getQuadraticZeroVector(d.get(0).getField(), param, d.getLength());
				for (int i = 0; i < messages.size(); i++) {
					d = messages.get(i).getProofs()[j].getD();
					c = messages.get(i).getProofs()[j].getC();

					D_sum = D_sum.add(d);
					C_sum = C_sum.add(c);

					gamma = messages.get(i).getStatements().get(j).getGamma();

					ftr_lhs_p.add(executor.submit(new PairingVector(c, gamma.multiply(d), crs)));
				}
				a = messages.get(0).getStatements().get(j).getA();
				b = messages.get(0).getStatements().get(j).getB();
				
				ftr_lhs_p.add(executor.submit(new PairingVector(crs.iota(1, a), D_sum, crs)));
				ftr_lhs_p.add(executor.submit(new PairingVector(C_sum, crs.iota(2, b), crs)));

			}

			Vector pi = messages.get(0).getProofs()[2].getProofs().get(0).getPi();

			Vector pi_sum = Vector.getQuadraticZeroVector(pi.get(0).getField(), param, pi.getLength());

			Vector theta = messages.get(0).getProofs()[2].getProofs().get(0).getTheta();
			Vector theta_sum = Vector.getQuadraticZeroVector(theta.get(0).getField(), param, theta.getLength());

			Element t;

			for (int j = 2; j < 6; j++) {
				t = messages.get(0).getStatements().get(j).getT();
				rhs_p = rhs_p.add(crs.iotaT(ProblemType.PAIRING_PRODUCT, t));

				for (int i = 0; i < messages.size(); i++) {
					pi = messages.get(i).getProofs()[j].getProofs().get(0).getPi();
					pi_sum = pi_sum.add(pi);
					theta = messages.get(i).getProofs()[j].getProofs().get(0).getTheta();
					theta_sum = theta_sum.add(theta);

				}
			}

			ftr_rhs_p.add(executor.submit(new PairingVectorPPP(pppU, pi_sum, crs)));

		ftr_rhs_p.add(executor.submit(new PairingVector(theta_sum, crs.getU2(), crs)));

			if (!lhs_m.sub(rhs_m).isZero()) {
				return false;
			}

			for (int i = 0; i < ftr_lhs_p.size(); i++) {
				try {
					lhs_p = lhs_p.add(ftr_lhs_p.get(i).get());
				} catch (InterruptedException | ExecutionException e) {

					e.printStackTrace();
				}
			}
			for (int i = 0; i < ftr_rhs_p.size(); i++) {
				try {
					rhs_p = rhs_p.add(ftr_rhs_p.get(i).get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

			return lhs_p.sub(rhs_p).isZero();
		} catch (Exception e) {
			System.out.println(e);
			return false;
		}
	}
	
	
	/** Agg_Verify Algorithm **/
	
	public boolean Agg_Verify(MessageSigned message, CommonReferenceString crs) {
		try {

			Element lhs_m = new QuarticElement(crs.getBT(), crs.getGT().newZeroElement(), crs.getGT().newZeroElement(),
					crs.getGT().newZeroElement(), crs.getGT().newZeroElement());
			Element rhs_m = new QuarticElement(crs.getBT(), crs.getGT().newZeroElement(), crs.getGT().newZeroElement(),
					crs.getGT().newZeroElement(), crs.getGT().newZeroElement());

			List<Future<Element>> ftr_lhs_m = new ArrayList<Future<Element>>();
			List<Future<Element>> ftr_rhs_m = new ArrayList<Future<Element>>();

				
				Proof proof = message.getProofs()[0];
				Statement statement = message.getStatements().get(0);
				SingleProof singleProof = proof.getProofs().get(0);
				Proof proof2 = message.getProofs()[1];
				Statement statement2 = message.getStatements().get(1);
				SingleProof singleProof2 = proof2.getProofs().get(0);


				ftr_rhs_m.add(executor.submit(new PairingVectorPPP(pppU, singleProof.getPi(), crs)));

			ftr_rhs_m.add(executor.submit(new PairingVector(singleProof.getTheta(), crs.getU2(), crs))); 


				ftr_rhs_m.add(executor.submit(new PairingVectorPPP(pppU, singleProof2.getPi(), crs)));

			ftr_rhs_m.add(executor.submit(new PairingVector(singleProof2.getTheta(), crs.getU2(), crs)));

				ftr_lhs_m.add(executor
						.submit(new PairingVector(proof.getC(), statement.getGamma().multiply(proof.getD()), crs)));
				ftr_lhs_m.add(executor
						.submit(new PairingVector(proof2.getC(), statement2.getGamma().multiply(proof2.getD()), crs)));

			

			for (int i = 0; i < ftr_lhs_m.size(); i++) {
				try {
					lhs_m = lhs_m.add(ftr_lhs_m.get(i).get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
			for (int i = 0; i < ftr_rhs_m.size(); i++) {
				try {
					rhs_m = rhs_m.add(ftr_rhs_m.get(i).get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

			Element lhs_p = new QuarticElement(crs.getBT(), crs.getGT().newZeroElement(), crs.getGT().newZeroElement(),
					crs.getGT().newZeroElement(), crs.getGT().newZeroElement());
			Element rhs_p = new QuarticElement(crs.getBT(), crs.getGT().newZeroElement(), crs.getGT().newZeroElement(),
					crs.getGT().newZeroElement(), crs.getGT().newZeroElement());

			Vector a;
			Vector b;
			Vector c;
			Vector d;
			Vector C_sum;
			Vector D_sum;
			Matrix gamma;

			List<Future<Element>> ftr_lhs_p = new ArrayList<Future<Element>>();
			List<Future<Element>> ftr_rhs_p = new ArrayList<Future<Element>>();

			for (int j = 2; j < 6; j++) {
				c = message.getProofs()[j].getC();
				d = message.getProofs()[j].getD();
				C_sum = Vector.getQuadraticZeroVector(c.get(0).getField(), param, c.getLength());
				D_sum = Vector.getQuadraticZeroVector(d.get(0).getField(), param, d.getLength());
			
					d = message.getProofs()[j].getD();
					c = message.getProofs()[j].getC();

					D_sum = D_sum.add(d);
					C_sum = C_sum.add(c);

					gamma = message.getStatements().get(j).getGamma();

					ftr_lhs_p.add(executor.submit(new PairingVector(c, gamma.multiply(d), crs)));
				
				a = message.getStatements().get(j).getA();
				b = message.getStatements().get(j).getB();
				
				ftr_lhs_p.add(executor.submit(new PairingVector(crs.iota(1, a), D_sum, crs)));
				ftr_lhs_p.add(executor.submit(new PairingVector(C_sum, crs.iota(2, b), crs)));

			}

			Vector pi = message.getProofs()[2].getProofs().get(0).getPi();

			Vector pi_sum = Vector.getQuadraticZeroVector(pi.get(0).getField(), param, pi.getLength());

			Vector theta = message.getProofs()[2].getProofs().get(0).getTheta();
			Vector theta_sum = Vector.getQuadraticZeroVector(theta.get(0).getField(), param, theta.getLength());

			Element t;

			for (int j = 2; j < 6; j++) {
				t = message.getStatements().get(j).getT();
				rhs_p = rhs_p.add(crs.iotaT(ProblemType.PAIRING_PRODUCT, t));

				
					pi = message.getProofs()[j].getProofs().get(0).getPi();
					pi_sum = pi_sum.add(pi);
					theta = message.getProofs()[j].getProofs().get(0).getTheta();
					theta_sum = theta_sum.add(theta);

				
			}

			ftr_rhs_p.add(executor.submit(new PairingVectorPPP(pppU, pi_sum, crs)));

		ftr_rhs_p.add(executor.submit(new PairingVector(theta_sum, crs.getU2(), crs)));

			if (!lhs_m.sub(rhs_m).isZero()) {
				return false;
			}

			for (int i = 0; i < ftr_lhs_p.size(); i++) {
				try {
					lhs_p = lhs_p.add(ftr_lhs_p.get(i).get());
				} catch (InterruptedException | ExecutionException e) {

					e.printStackTrace();
				}
			}
			for (int i = 0; i < ftr_rhs_p.size(); i++) {
				try {
					rhs_p = rhs_p.add(ftr_rhs_p.get(i).get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

			return lhs_p.sub(rhs_p).isZero();
		} catch (Exception e) {
			System.out.println(e);
			return false;
		}
	}
	
	
	
	
	
	



	private class PairingVector implements Callable<Element> {
		/** Tâche permettant de faire le pairing de deux vecteurs -> e(v1,v2) **/
		private Vector v1;
		private Vector v2;
		private CommonReferenceString crs;

		public PairingVector(Vector v1, Vector v2, CommonReferenceString crs) {
			this.v1 = v1;
			this.v2 = v2;
			this.crs = crs;
		}

		@Override
		public Element call() throws Exception {
			Element output = v1.pairInB(v2, crs.getPairing());
			return output;
		}

	}

	private class PairingVectorPPP implements Callable<Element> {
		/**
		 * Tâche permettant de faire le pairing d'un vecteur préparé et d'un autre ->
		 * e(ppp(v1),v2)
		 **/
		private VectorPPP vppp;
		private Vector v2;
		private CommonReferenceString crs;

		public PairingVectorPPP(VectorPPP vppp, Vector v2, CommonReferenceString crs) {
			this.vppp = vppp;
			this.v2 = v2;
			this.crs = crs;
		}

		@Override
		public Element call() throws Exception {
			Element output = vppp.pairing(v2);
			return output;
		}
	}



}
