TextNormalizationPolicy
======================

status=stable
scope=core
audience=core / CNCF / infra / AI integration

NOTE:
This document is intentionally written in Japanese.
Reason: Japanese/CJK text normalization semantics cannot be specified
precisely in English without semantic loss, especially for AI reasoning.

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

TextNormalizationPolicy は、文字列をどのように正規化し、
「意味的に同一」と見なすかを定義するためのポリシーである。

本ポリシーは、表示や翻訳を目的とせず、
知識処理・検索・同一性判定・AI/RAG 前処理を主用途とする。

----------------------------------------------------------------------
2. Policy Set
----------------------------------------------------------------------

TextNormalizationPolicy は、少なくとも以下のポリシーを提供する。

- JapaneseCanonical
- StrictBinary

----------------------------------------------------------------------
2.1 TextNormalizationPolicy: JapaneseCanonical
----------------------------------------------------------------------

### Purpose

JapaneseCanonical は、日本語（漢字圏）における知識処理のために、
表記揺れを吸収し、意味的に安定した正規形を得ることを目的とする。

### Normative Definition

JapaneseCanonical は、入力文字列に対して以下を必ず適用する。

Unicode 正規化:
- Unicode 正規化形式として NFKC を適用する

カナ正規化:
- 半角カナは入力として禁止とみなす
- ただし正規化処理では必ず全角カナに変換する
- 濁点・半濁点は結合済みの正規形に統一する

数字の正規化:
- 数字は常に ASCII 半角数字（0–9）に正規化する

英字の正規化:
- 全角英字は半角英字に正規化する
- 大文字・小文字の扱いは TextComparisonPolicy に委ねる

記号:
- 記号の全角／半角差は NFKC に委ねる
- 記号の意味的同一視は本ポリシーでは規定しない

### Explicit Non-Goals

JapaneseCanonical は以下を行わない。

- 表示用文字列の生成
- 翻訳・言語変換
- 文法解析・形態素解析
- 旧字体／新字体の統合
- 入力バリデーション

### Typical Use Cases

- 検索インデックス生成
- ユーザー入力の検索前処理
- 重複検出・同一性判定
- 正規化キー生成
- AI / RAG / Embedding 前処理

----------------------------------------------------------------------
2.2 TextNormalizationPolicy: StrictBinary
----------------------------------------------------------------------

### Purpose

StrictBinary は、文字列を一切正規化せず、
入力された文字列をそのまま扱うためのポリシーである。

### Normative Definition

StrictBinary は、以下を必ず保証する。

- Unicode 正規化を行わない
- 全角／半角変換を行わない
- カナ正規化を行わない
- 英字・数字・記号の変換を行わない

すなわち：

normalize(text, StrictBinary) == text

### Explicit Non-Goals

StrictBinary は以下を一切行わない。

- 表記揺れの吸収
- 日本語向け意味的同一化
- 検索補助

### Typical Use Cases

- パスワード
- トークン
- API キー
- バイナリ ID
- プロトコルレベル文字列

----------------------------------------------------------------------
3. Design Rationale
----------------------------------------------------------------------

日本語・漢字圏においては、Locale や JVM 標準機能のみでは
意味的同一性を保証できない。

JapaneseCanonical と StrictBinary を併存させることで、
知識処理用途とセキュリティ用途を安全に分離する。

END
----------------------------------------------------------------------
