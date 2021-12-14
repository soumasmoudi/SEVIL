/*
 * Copyright (c) 2016 Gijs Van Laer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package edu.jhu.isi.grothsahai.api.impl;

import edu.jhu.isi.grothsahai.api.Verifier;
import edu.jhu.isi.grothsahai.entities.CommonReferenceString;
import edu.jhu.isi.grothsahai.entities.Proof;
import edu.jhu.isi.grothsahai.entities.Statement;
import edu.jhu.isi.grothsahai.entities.Vector;
import edu.jhu.isi.grothsahai.entities.QuarticElement;
import edu.jhu.isi.grothsahai.entities.SingleProof;
import edu.jhu.isi.grothsahai.enums.ProblemType;
import edu.jhu.isi.grothsahai.json.Serializer;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.PairingPreProcessing;
import utilities.VectorPPP;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VerifierImpl implements Verifier {

	private CommonReferenceString crs;

	private VectorPPP pppU;// Les preprocessing des vecteurs U et V
//	private VectorPPP pppV; //preprocessing ne fonctionne pas sur V avec le type F

	private final ExecutorService executor;

	public VerifierImpl(final CommonReferenceString crs, final ExecutorService executor) {
		this.crs = crs;
		this.pppU = new VectorPPP(crs.getU1(), crs);
//		this.pppV = new VectorPPP(crs.getU2(), crs); //preprocessing ne fonctionne pas sur V avec le type F

		this.executor = executor;
	}

	public Boolean verify(final List<Statement> statements, final Proof proof) {

		if (proof.getProofs().size() != statements.size()) {
			System.out.println("pb de taille");
			return false;
		}

		List<Future<Boolean>> ftr_bools = new ArrayList<Future<Boolean>>();
		for (int i = 0; i < proof.getProofs().size(); i++) {

			ftr_bools.add(executor.submit(new VerifEq(proof, statements.get(i), proof.getProofs().get(i))));

		}

		for (int i = 0; i < proof.getProofs().size(); i++) {

			try {
				if (!ftr_bools.get(i).get()) {
					return false;
				} else {

				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}

		}
		return true;
	}

	/**
	 * Fonction provenant de groth sahai, modifiée pour inclure la parallélisation
	 **/
	private boolean verifyOneEquation(final Proof proof, final Statement statement, final SingleProof singleProof) {
		Callable<Element> call_lhs = new Callable<Element>() {// Calcule l'élément de gauche de l'équation
			@Override
			public Element call() throws Exception {
				Element lhs;
				if (statement.getA().getLength() != 0) {
//					lhs = statement.getPppA().pairing(proof.getD()); //Le PPP de A et de B ne fonctionne pas
					lhs = crs.iota(1, statement.getA()).pairInB(proof.getD(), crs.getPairing());
				} else {
					lhs = new QuarticElement(crs.getBT(), crs.getGT().newZeroElement(), crs.getGT().newZeroElement(),
							crs.getGT().newZeroElement(), crs.getGT().newZeroElement());
				}
				if (statement.getB().getLength() != 0) {

//					lhs = lhs.add(statement.getPppB().pairingreverse(proof.getC())); //Le PPP de A et de B ne fonctionne pas
					lhs = lhs.add(proof.getC().pairInB(crs.iota(2, statement.getB()), crs.getPairing()));
				}
				if (statement.getGamma() != null) {
					lhs = lhs.add(proof.getC().pairInB(statement.getGamma().multiply(proof.getD()), crs.getPairing()));
				}

				return lhs;
			}
		};
		Future<Element> ftr_lhs = executor.submit(call_lhs);

		Callable<Element> call_rhs = new Callable<Element>() {// Calcule l'élément de droite de l'équation
			@Override
			public Element call() throws Exception {
//				Element output = crs.iotaT(ProblemType.PAIRING_PRODUCT, statement.getT())  //Sans PPP
//		                .add(crs.getU1().pairInB(singleProof.getPi(), crs.getPairing()))
//		                .add(singleProof.getTheta().pairInB(crs.getU2(), crs.getPairing()));
				
//				Element output = crs.iotaT(ProblemType.PAIRING_PRODUCT, statement.getT())// Avec PPP de U et V
//						.add(pppU.pairing(singleProof.getPi())).add(pppV.pairingreverse(singleProof.getTheta()));

				Element output = crs.iotaT(ProblemType.PAIRING_PRODUCT, statement.getT())
						.add(pppU.pairing(singleProof.getPi())).add(singleProof.getTheta().pairInB(crs.getU2(), crs.getPairing()));//preprocessing ne fonctionne pas sur V avec le type F
				
				
				return output;
			}
		};
		Future<Element> ftr_rhs = executor.submit(call_rhs);

		Element rhs = null;
		Element lhs = null;

		try {// On attend que les calculs soient finis puis on récupère les valeurs
			lhs = ftr_lhs.get();
			rhs = ftr_rhs.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		return (lhs.sub(rhs)).isZero();

	}

	private class VerifEq implements Callable<Boolean> {// Tâche permettant de vérifier une équation en parallèle

		private Statement statement;
		private Proof proof;
		private SingleProof singleproof;

		public VerifEq(Proof proof, Statement statement, SingleProof singleproof) {

			this.statement = statement;
			this.proof = proof;
			this.singleproof = singleproof;
		}

		@Override
		public Boolean call() throws Exception {
			boolean bool = verifyOneEquation(proof, statement, singleproof);
			return bool;
		}

	}
}
