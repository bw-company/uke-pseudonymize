# uke-pseudonymize

[![.github/workflows/build.yml](https://github.com/bw-company/uke-pseudonymize/actions/workflows/build.yml/badge.svg)](https://github.com/bw-company/uke-pseudonymize/actions/workflows/build.yml)

UKEファイルを仮名加工情報に変換するツールです。

> [仮名加工情報は、他の情報と照合しない限り特定の個人を識別できないように加工した個人に関する情報（法第２条第５項）であり、仮名加工情報を作成した個人情報取扱事業者においては、通常、当該仮名加工情報の作成の元となった個人情報や当該仮名加工情報に係る削除情報等を保有していると考えられることから、原則として「個人情報」（法第２条第１項）に該当するものです。](https://www.ppc.go.jp/all_faq_index/faq1-q14-1/)
> [変更前の利用目的と関連性を有すると合理的に認められる範囲を超える利用目的の変更が可能ですが（法第41条第９項）、原則として第三者への提供が禁止されています（法第41条第６項）](https://www.ppc.go.jp/all_faq_index/faq1-q14-1/)

[個人情報の保護に関する法律についてのガイドライン（仮名加工情報・匿名加工情報編）](https://www.ppc.go.jp/personalinfo/legal/guidelines_anonymous/#a2-2)に従い、[法第41条第1項に規定する個人情報の保護に関する法律施行規則（平成28年個人情報保護委員会規則第3号）](https://www.ppc.go.jp/files/pdf/290530_personal_commissionrules.pdf)で定める基準に従って加工することを目指しています。

作成した仮名加工情報は第三者に提供できません。また、現時点ではHENファイルには対応していません。

## 開発方法

ビルドにGradleを使っています。
`build` タスクを実行することでビルドと成果物作成、テストといった一連の作業を自動実行できます。

フォーマットにはSpotlessを使っています。
`spotlessApply` タスクを実行することでKotlinコードのフォーマットを行えます。

## 著作権表記

Copyright &copy; 2022-2024 Henry, Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
