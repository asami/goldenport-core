TextComparisonPolicy
====================

status=stable
scope=core
audience=core / CNCF / infra / AI integration

NOTE:
This document is intentionally written in Japanese.
Reason: Japanese/CJK comparison semantics cannot be specified
precisely in English without semantic loss.

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

TextComparisonPolicy は、2 つの文字列をどの規則で比較・並び替え、
同一性を判断するかを定義する。

本ポリシーは TextNormalizationPolicy と独立した概念であり、
正規化後の文字列に対して適用されることを前提とする。

----------------------------------------------------------------------
2. Policy Set
----------------------------------------------------------------------

- JapaneseCollation
- BinaryCompare

----------------------------------------------------------------------
2.1 TextComparisonPolicy: JapaneseCollation
----------------------------------------------------------------------

### Purpose

JapaneseCollation は、日本語（漢字圏）における
人間の期待に沿った比較・並び順を提供する。

### Normative Definition

- 五十音順を基本とする
- 濁音・半濁音は基底音との関係を考慮する
- 長音・小書き文字は言語的規則に従う
- 比較は TextNormalizationPolicy 適用後に行われる

### Explicit Non-Goals

- 正規化方法の定義
- 記号の意味的同一視
- 厳密なバイト列比較

### Typical Use Cases

- 検索結果の並び替え
- 日本語 UI の一覧表示
- 知識グラフ上の概念ソート

----------------------------------------------------------------------
2.2 TextComparisonPolicy: BinaryCompare
----------------------------------------------------------------------

### Purpose

BinaryCompare は、文字列をバイト列として厳密に比較する。

### Normative Definition

- JVM の String.equals / compareTo に準拠する
- 言語的意味を考慮しない
- 比較結果は完全に決定的である

### Typical Use Cases

- トークン比較
- ID / キー比較
- セキュリティ関連処理

----------------------------------------------------------------------
3. Relationship with TextNormalizationPolicy
----------------------------------------------------------------------

典型的な組み合わせ：

- JapaneseCanonical + JapaneseCollation : 知識処理・検索
- JapaneseCanonical + BinaryCompare     : 正規化後キー比較
- StrictBinary + BinaryCompare          : セキュリティ用途

----------------------------------------------------------------------
4. Design Rationale
----------------------------------------------------------------------

比較規則を暗黙にせず、宣言的に定義することで、
日本語処理とセキュリティ処理を明確に分離できる。

END
----------------------------------------------------------------------
