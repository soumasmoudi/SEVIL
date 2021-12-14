package utilities;

import java.util.List;

import edu.jhu.isi.grothsahai.entities.Proof;
import edu.jhu.isi.grothsahai.entities.Statement;
import it.unisa.dia.gas.jpbc.Element;

public class Message {
	
	private Element M;
	private Proof[] proofs;
	private List<Statement> statements;

	public Message(Element m, Proof[] proofs, List<Statement> statements) {

		this.M = m;
		this.proofs = proofs;
		this.statements = statements;
		
	}

	public Element getM() {
		return M;
	}

	public Proof[] getProofs() {
		return proofs;
	}

	public List<Statement> getStatements() {
		return statements;
	}

}
