package com.genSci.tools.TexToClozeCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class PrimaryController {
	String[] itemArray = { "標準問題", "数値問題" };
	ObservableList<String> availableChoices = FXCollections.observableArrayList(itemArray);
	String str;
	@FXML
	TextArea srcArea;

	@FXML
	TextArea codeArea;
	@FXML
	ChoiceBox<String> choice;

	@FXML
	private void clearSrcArea() {
		srcArea.clear();
	}

	@FXML
	private void clearCodeArea() {
		codeArea.clear();
	}

	@FXML
	private void copyToClipboad() {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(codeArea.getText());
		clipboard.setContent(content);
	}

	@FXML
	private void initialize() {
		choice.setItems(availableChoices);
		choice.setValue(availableChoices.get(0));
	}

	@FXML
	private void quitAction() {
		System.exit(0);
	}

	@FXML
	private void execAction() {
		// choice ボックス値で処理を分ける。
		String selected = choice.getValue();
		if (selected == null)
			return;

		if (selected.equals(itemArray[0])) {
			standardCode();
		}
		if (selected.equals(itemArray[1])) {
			calculusCode();
		}
	} // end of execAction()
		//

	private void standardCode() {
		str = srcArea.getText();
		// \begin{enumerate} までの「問題文」を切り出す。
		String questionFirst = null;
		String regex = "(^.+?)\\\\begin";
		Pattern p = Pattern.compile(regex, Pattern.DOTALL);
		Matcher m = p.matcher(str);
		if (m.find()) {
			questionFirst = m.group(1);
		}
		questionFirst = changeMathCode(questionFirst);
		// 最初の問題文を書き出す。
		codeArea.appendText(questionFirst + "\n");

		// \begin{enumerate}から\end{enumerate}までを取り出せるか
		String beginStr = "\\\\begin\\{enumerate}";
		String endStr = "\\\\end\\{enumerate}";
		regex = beginStr + "(.+?)" + endStr;
		p = Pattern.compile(regex, Pattern.DOTALL);
		m = p.matcher(str);
		String questionStr = null;
		while (m.find()) {
			questionStr = m.group(1).trim();
			questionStr = questionStr.replaceAll("\t", "");
			questionStr = questionStr.replaceAll("\\\\item", "<li>");
			questionStr = questionStr.replaceAll("\n", "</li>\n");
			// trim()で末尾を消してしまっているので末尾に付加
			questionStr = questionStr + "</li>\n";
			// modArea.appendText(questionStr+"\n");
		}
		questionStr = changeMathCode(questionStr);
		// ここまででenumerate環境で{\toi}を含んだ文がquestionStr に保存

		// tabular 環境下の「選択肢」を保存
		regex = "選択肢.+?\\\\begin\\{tabular}\\{[clr]+}(.+?)\\\\end\\{tabular}";
		p = Pattern.compile(regex, Pattern.DOTALL);
		m = p.matcher(str);
		String selectStr = null;
		while (m.find()) {
			selectStr = m.group(1);
			// modArea.appendText(selectStr + "\n");
		}
		// ここまでで、selectStr に選択肢番号とその内容が入る。
		//数式コードの変換
		// selectStr を区切って内容を List に保存する。
		// \ban{*}を削除
		selectStr = selectStr.replaceAll("\\\\ban\\{.+?}", "");
		// modArea.appendText(selectStr+"\n");
		// 2行以上の時は文末に「\\」+改行コードが入っているのでこれを&に変換
		selectStr = selectStr.replaceAll("\\\\.+?\\n", "&");
		// 前後の空白・改行コードを消す
		selectStr = selectStr.trim();
		// 文末の&を消す。
		selectStr = selectStr.replaceAll("(?!^)&+$", "");
		// modArea.appendText(selectStr + "\n");
		// これで選択肢が&区切りの String になったので、List に内容を格納
		List<String> selectList = new ArrayList<String>();
		String[] selectArray = selectStr.split("&");
		for (String s : selectArray) {
			// 内容が数式の場合はSSを\(\) に変える必要
			regex = "\\$(.+?)\\$";
			p = Pattern.compile(regex);
			m = p.matcher(s);
			while (m.find()) {
				String ss = m.group(1);
				s = "\\(" + ss + "\\)";
			}
			selectList.add(s.trim());
		}
		// 「選択肢」の処理終わり

		// 続いて、正解についても同じように処理をする。
		regex = "正解.+?\\\\begin\\{tabular}\\{[clr]+}(.+?)\\\\end\\{tabular}";
		p = Pattern.compile(regex, Pattern.DOTALL);
		m = p.matcher(str);
		String ansStr = null;
		while (m.find()) {
			ansStr = m.group(1);
		}
		// {\toi}を削除する
		ansStr = ansStr.replaceAll("\\{\\\\toi}", "");
		// \ban{}を削除して、中身だけを残す
		regex = "\\\\ban\\{(.+?)}";
		p = Pattern.compile(regex);
		m = p.matcher(ansStr);
		List<String> ansList = new ArrayList<String>();
		while (m.find()) {
			String ansNum = m.group(1);
			// modArea.appendText(ansNum+"\n");
			ansList.add(ansNum.trim());
		}
		// ここまでで「選択肢内容」「正解番号」がListに保存されている。
		// 正解番号と選択肢内容を対応させ 正解内容だけをList にする。
		List<String> ansItemList = new ArrayList<String>();
		for (String s : ansList) {
			int ansNum = Integer.parseInt(s);
			int ansPos = ansNum - 1; // selectList の場所
			// modArea.appendText(s+":"+ selectList.get(ansPos)+"\n");
			ansItemList.add(selectList.get(ansPos));
		}
		// 「正解」処理の終わり。

		// 選択肢をランダムに並べ替える。
		Collections.shuffle(selectList);
		// 並び替えた選択肢に基づいて正解番号を生成ansListを作り替え
		int count = 0;
		for (String s : ansItemList) {
			for (int i = 0; i < selectList.size(); i++) {
				String s2 = selectList.get(i);
				if (s.contentEquals(s2)) {
					ansList.set(count, "" + (i + 1));
					// System.out.println(s+":"+s2+"num="+(i+1));
					count++;
				} // end of if(...
			} // end of for(.. 内ループ
		} // end of for(... 外ループ
			// ansList は並び替えられた選択肢に基づいて対応が取られた

		// 問題の{\toi}に正解番号を埋め込んでいく。
		// questionStr から{\toi}を探す。
		regex = "\\{\\\\toi}";
		p = Pattern.compile(regex);
		m = p.matcher(questionStr);
		count = 0;
		while (m.find()) {
			String rep = "{1:SA:=" + ansList.get(count) + "}";
			// modArea.appendText("rep="+rep+"\n");
			questionStr = questionStr.replaceFirst(regex, rep);
			count++;
		}
		// Cloze コードが埋め込まれた問題文の完成。

		// ここで問題リストを書き出す
		codeArea.appendText("<ol>\n");
		codeArea.appendText(questionStr);
		codeArea.appendText("</ol>\n");

		//
		// 選択tableをつくる。
		String tableStr = "<table>\n<caption><b>選択肢</b></caption>\n";
		// 選択肢の数
		int num = selectList.size();
		int index = 0;
		tableStr += ("<tr>");
		while (index < num) {
			tableStr += "<td>" + (index + 1) + ". " + selectList.get(index) + "</td>";
			if (((index + 1) % 5) == 0) {
				tableStr += ("</tr>\n");
				tableStr += ("<tr>");
			}
			if (index == num - 1) {
				tableStr += ("</tr>\n");
			}
			index++;
		}
		tableStr += ("</table>\n");
		// <table>...</table>で整形された選択肢を出力する。
		codeArea.appendText(tableStr + "\n");

	}// end of standardCode()
		//

	private void calculusCode() {
		// 数値問題の場合は「選択肢」がない、と仮定する。もし問題にあった場合はそれを処理しない。
		str = srcArea.getText();
		// \begin{enumerate} までの「問題文」を切り出す。
		String questionFirst = null;
		String regex = "(^.+?)\\\\begin";
		Pattern p = Pattern.compile(regex, Pattern.DOTALL);
		Matcher m = p.matcher(str);
		if (m.find()) {
			questionFirst = m.group(1);
		}
		//Tex の数式コードを Cloze コードに変換する
		questionFirst = changeMathCode(questionFirst);
		// 最初の問題文を書き出す。
		codeArea.appendText(questionFirst + "\n");

		//// \begin{enumerate}から\end{enumerate}までを取り出せるか
		String beginStr = "\\\\begin\\{enumerate}";
		String endStr = "\\\\end\\{enumerate}";
		regex = beginStr + "(.+?)" + endStr;
		p = Pattern.compile(regex, Pattern.DOTALL);
		m = p.matcher(str);
		String questionStr = null;
		while (m.find()) {
			questionStr = m.group(1).trim();
			questionStr = questionStr.replaceAll("\t", "");
			questionStr = questionStr.replaceAll("\\\\item", "<li>");
			questionStr = questionStr.replaceAll("\n", "</li>\n");
			// trim()で末尾を消してしまっているので末尾に付加
			questionStr = questionStr + "</li>\n";
			// codeArea.appendText(questionStr + "\n");
		}
		questionStr = changeMathCode(questionStr);
		// ここまででenumerate環境で{\toi}を含んだ文がquestionStr に保存

		// 数値問題の場合、正解の配列を先に用意しておく必要がある。
		regex = "正解.+?\\\\begin\\{tabular}\\{[clr]+}(.+?)\\\\end\\{tabular}";
		p = Pattern.compile(regex, Pattern.DOTALL);
		m = p.matcher(str);
		String ansStr = null;
		while (m.find()) {
			ansStr = m.group(1);
		}
		// {\toi}を削除する
		ansStr = ansStr.replaceAll("\\{\\\\toi}", "");
		// \ban{}を削除して、中身だけを残す
		regex = "\\\\ban\\{(.+?)}";
		p = Pattern.compile(regex);
		m = p.matcher(ansStr);
		List<String> ansList = new ArrayList<String>();
		while (m.find()) {
			String ansNum = m.group(1);
			// 数値問題の場合、「10」は0である。
			if (ansNum.contentEquals("10")) {
				//codeArea.appendText("find10\n");
				ansNum = "0";
			}
			//codeArea.appendText(ansNum + "\n");
			ansList.add(ansNum.trim());
		}
		/*
		for (String s : ansList) {
			codeArea.appendText(s + "\n");
		}
		*/
		// 数値問題の場合、{\toi}が連続する可能性がある。それを見つけられるか？
		// questionStr から{\toi}を探す。
		regex = "(\\{\\\\toi})+";
		p = Pattern.compile(regex);
		m = p.matcher(questionStr);
		String toi = "{\\toi}";
		int toiLength = toi.length();
		// ダミー正解。問題数6
		//String[] dummy = { "1", "2", "3", "4", "5", "6", "7" };
		int ansCounter = 0; // 正解配列をカウントする
		while (m.find()) {
			String s = m.group();
			//codeArea.appendText(s + "\n");
			//
			int toiCount = 1;// 連続している{\toi}の数
			if (s.length() > toiLength) {
				// {\toi}が2個以上続いている。
				int L = toiLength;
				while (L < s.length()) {
					L += toiLength;
					toiCount++;
				}
			}
			// codeArea.appendText("length="+s.length()+"count:"+toiCount+"\n");
			// 正解をtoiCount ずつ置き換える。
			String rep = "";
			for (int i = 0; i < toiCount; i++) {
				rep += ansList.get(ansCounter);
				// System.out.println("i="+i+"\tansCounter="+ansCounter);
				ansCounter++;
			}
			//codeArea.appendText("rep=" + rep + "\n");
			rep = "{1:SA:=" + rep + "}";
			questionStr = questionStr.replaceFirst(regex, rep);
		} // end of while(m.find()...
		codeArea.appendText("<ol>\n");
		codeArea.appendText(questionStr);
		codeArea.appendText("</ol>\n");
	} // end of calculusCode()
		//

	private String changeMathCode(String str) {
		// $$を\(\)に変える。
		String regex = "\\$(.+?)\\$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(str);
		while (m.find()) {
			String ss = m.group(1);
			String rep = "\\\\(" + ss + "\\\\)";
			str = str.replaceFirst(regex, rep);
		}
		// \[ \] かもしれない。
		regex = "\\[(.+?)\\\\]";
		p = Pattern.compile(regex, Pattern.DOTALL);
		m = p.matcher(str);
		while (m.find()) {
			String ss = m.group(1);
			// codeArea.appendText("find:"+ss+"\n");
			String rep = "\\\\(" + ss + "\\\\)";
			str = str.replaceFirst("\\[", "(");
			str = str.replaceFirst("\\]", ")");
		}
		return str;
	}

}
