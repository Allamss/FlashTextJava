package com.sperske.jason.flashtext;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * FlashTextJava - An idiomatic Java port of the Python library FlashText by Vikash Singh
 * Original Python source can be found at https://github.com/vi3k6i5/flashtext
 * Based on the Aho-Corasick algorithm (https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm)
 * Java Version written by Jason Sperske
 */
public class KeywordProcessor {
	// immutable properties once KeywordProcessor is instantiated
	private final boolean CASE_SENSITIVE;

	// dynamic properties while KeywordProcessor is being built up
	private int _terms_in_trie = 0;
	private KeywordTrieNode rootNode;

	public KeywordProcessor() {
		this(false);
	}

	public KeywordProcessor(boolean case_sensitive) {
		this.CASE_SENSITIVE = case_sensitive;
		this.rootNode = new KeywordTrieNode();
	}

	public int length() {
		return this._terms_in_trie;
	}

	public boolean contains(String word) {
		KeywordTrieNode current_keyword_trie_node = this.rootNode;
		int chars_traveled = 0;

		if (!this.CASE_SENSITIVE) {
			word = word.toLowerCase();
		}
		for (Character c : word.toCharArray()) {
			if (current_keyword_trie_node.contains(c)) {
				current_keyword_trie_node = current_keyword_trie_node.children.get(c);
				chars_traveled += 1;
			} else {
				return false;
			}
		}

		return chars_traveled == word.length() && current_keyword_trie_node.contains(word);
	}

	public String get(String word) {
		KeywordTrieNode current_keyword_trie_node = this.rootNode;
		int chars_traveled = 0;

		if (!this.CASE_SENSITIVE) {
			word = word.toLowerCase();
		}
		for(Character c : word.toCharArray()) {
			if (current_keyword_trie_node.contains(c)) {
				current_keyword_trie_node = current_keyword_trie_node.children.get(c);
				chars_traveled += 1;
			} else {
				return null;
			}
		}

		if (chars_traveled == word.length()) {
			return current_keyword_trie_node.get();
		} else {
			return null;
		}
	}

	public void addKeyword(String word) {
		// Clean Name is set to word when not defined
		addKeyword(word, word);
	}

	public void addKeyword(String word, String clean_name) {
		if (!this.CASE_SENSITIVE) {
			word = word.toLowerCase();
		}
		LinkedList<Character> characters = word.chars().mapToObj(c -> (char)c).collect(Collectors.toCollection(LinkedList::new));

		this.rootNode.add(characters, word, clean_name);
		this._terms_in_trie += 1;
	}

	public Set<String> extractKeywords(String sentence) {
		HashSet<String> foundKeywords = new HashSet<>();
		if (sentence == null || sentence.isEmpty()) return foundKeywords;

		if (!this.CASE_SENSITIVE)
			sentence = sentence.toLowerCase();
		char[] charArray = sentence.toCharArray();
		int i = 0;
		while (i < charArray.length) {
			char c = charArray[i];

			KeywordTrieNode nextNode = this.rootNode.get(c);
			if (nextNode == null) {
				i++;
				continue;
			}

			Integer findSourceLength = null;
			String findWord = null;
			int j = i;
			while (nextNode != null) {
				if (nextNode.getKeyword() != null) {
					findSourceLength = nextNode.getKeyword().length();
					findWord = nextNode.getCleanName();
				}

				if (j == charArray.length - 1) break;

				c = charArray[++j];
				nextNode = nextNode.get(c);
			}

			if (findSourceLength == null) {
				i++;
				continue;
			}

			foundKeywords.add(findWord);
			i += findSourceLength;
		}
		return foundKeywords;
	}

	public String replace(String sentence) {
		if (sentence == null || sentence.isEmpty()) return sentence;

		if (!this.CASE_SENSITIVE)
			sentence = sentence.toLowerCase();
		StringBuilder sb = new StringBuilder();
		char[] charArray = sentence.toCharArray();
		int i = 0;
		while (i < charArray.length) {
			char c = charArray[i];

			KeywordTrieNode nextNode = this.rootNode.get(c);
			if (nextNode == null) {
				i++;
				sb.append(c);
				continue;
			}

			Integer findSourceLength = null;
			String findWord = null;
			int j = i;
			while (nextNode != null) {
				if (nextNode.getKeyword() != null) {
					findSourceLength = nextNode.getKeyword().length();
					findWord = nextNode.getCleanName();
				}

				if (j == charArray.length - 1) break;

				nextNode = nextNode.get(charArray[++j]);
			}

			if (findSourceLength == null) {
				i++;
				sb.append(c);
				continue;
			}

			sb.append(findWord);
			i += findSourceLength;
		}
		return sb.toString();
	}

	public String toString() {
		return this.rootNode.toString();
	}
}
