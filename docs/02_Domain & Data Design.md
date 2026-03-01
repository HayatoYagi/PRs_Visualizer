# **PRs Visualizer for GitHub: Domain & Data Design**

本ドキュメントでは、GitHub APIから取得したデータをどのように処理し、UIに反映させるかのデータ構造とアルゴリズムを定義する。

## **1\. ドメインモデル (Domain Models)**

### **1.1. リポジトリ構造 (Repository Tree)**

sealed class FileNode {  
    abstract val path: String  
    abstract val name: String  
    abstract var weight: Double // 面積計算の重み（行数など）

    data class Directory(  
        override val path: String,  
        override val name: String,  
        val children: MutableList\<FileNode\> \= mutableListOf(),  
        override var weight: Double \= 0.0  
    ) : FileNode()

    data class File(  
        override val path: String,  
        override val name: String,  
        val extension: String,  
        override var weight: Double \= 0.0,  
        val totalLines: Int,  
        var hasActivePr: Boolean \= false // 小さなノードの消失防止フラグ  
    ) : FileNode()  
}

## **2\. ズームと視認性の最適化 (Semantic Zoom Logic)**

「ズームすると小さなノードが見えなくなる」「階層が深いと辿り着けない」問題への対策。

### **2.1. レンダリング閾値 (Pixel-based Threshold)**

描画エンジン（Compose/Skia）側で、各 LayoutRect の実描画サイズを計算し、以下のルールを適用する。

* **Minimum Render Size (2x2px)**:  
  実描画サイズがこれ以下の場合は描画をスキップ（パフォーマンス向上）。ただし、hasActivePr \== true の場合は 1x1px のハイライト点として描画。  
* **Detail Fade-in (50x50px以上)**:  
  矩形がこのサイズを超えたら、その子要素（ディレクトリ内身）の描画を開始する。  
* **Label Visibility (width \> 80px かつ height \> 20px)**:  
  このサイズを確保できた場合のみ、ファイル名/ディレクトリ名を表示。

### **2.2. 適応型アルゴリズム**

ズーム中心点からの距離と、現在のスケールを掛け合わせて「実効面積」を算出する。

![][image1]この ![][image2] が一定値を超えたものから順次、深い階層を「掘り下げて」描画する。これにより、ズームしている箇所の周辺だけが詳しく見え、遠くの箇所は粗いまま（計算コスト低減）という挙動を実現する。

## **3\. UI State 設計 (View Model)**

data class VisualizerUiState(  
    val rootNode: FileNode.Directory?,  
    val layoutMap: Map\<String, LayoutRect\>,  
    val activePullRequests: List\<PullRequest\>,  
    val selectedPrIds: Set\<String\>,  
    val zoomLevel: Float \= 1.0f,  
    val panOffset: Offset \= Offset.Zero, // ズーム位置の移動  
    val focusPath: String \= "",   
    val isLoading: Boolean \= false  
)

data class LayoutRect(  
    val x: Float,  
    val y: Float,  
    val width: Float,  
    val height: Float,  
    val depth: Int,  
    val isImportant: Boolean // PR対象かどうか  
)

## **4\. データ変換フロー (Updated)**

1. **Fetch**: GitHub APIから構造とPRを取得。  
2. **Weighting**: LOCをベースに重み付け。PR対象ファイルには最低限の minWeight を保証し、極小ファイルでも認識可能にする。  
3. **Layout**: Squarified Treemapで座標算出。  
4. **Visibility Pre-calculation**:  
   * 現在の zoomLevel において「どのノードまでが詳細描画対象か」を事前にマーク。  
   * PR対象ノードは、親ディレクトリが小さくても「中に何かある」ことを示すインジケータ（光る枠線など）を親ノードに伝播させる。

## **5\. コンフリクト判定と強調**

複数のPRが同じファイルを変更している場合、そのノードはサイズに関わらず **"Priority Node"** として扱い、ズームアウト時でも視認できる特殊なエフェクト（警告色のグローなど）を付与する。

[image1]: <data:image/png;base64,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>

[image2]: <data:image/png;base64,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>