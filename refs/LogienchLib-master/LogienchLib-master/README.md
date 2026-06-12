# LogienchLib-v2-2
書き方や仕様が古くなった LogienchLibを進化させるために作成されました。  
複数のサーバーの作成経験をもとに、よく使う機能を使いやすい形で提供します。


# 使い方

> [!IMPORTANT]
> 2.0.0の部分は使用するバージョンに合わせて変更してください

> [!WARNING]
> 同じバージョンのものを途中で別の方法を使い読み込もうとすると、キャッシュの影響で正常に読み込まれない、反映されない可能性があります

## 簡単だけどjavadoc(コメント) が一部しか表示されない

プロジェクトのルートに`libs`ディレクトリを作成し、そこに以下のjarファイルをそのまま投下

- LogienchLib-2.0.0.jar
- LogienchLib-2.0.0-sources.jar

以下のコードを`build.gradle`に追加
> repositories

```gd
flatDir {
    dirs "libs"
}
```

> dependencies
```gd
compileOnly("net.logiench:LogienchLib:2.0.0")
```
> [!WARNING]
> LogienchLibはProject-Logiench専用の非公開ライブラリです。  
> 他のプロジェクトに使用する際は、必ず.gitignoreに指定し、コミットしないようにしてください。

## 難しいけど全てのjavadocが表示される

#### この方法を使用するための条件

- LogienchLib-v2-2のgithubへのアクセス権限があること
- 環境変数か、`C:\Users\ユーザー名\.gradle\gradle.properties`にアクセストークンを設定すること

### アクセストークンの設定

> `C:\Users\ユーザー名\.gradle\gradle.properties`

```properties
# GitHub Packages 認証情報
GITHUB_ACTOR=Logiench
GITHUB_TOKEN=ghp_**
```

`gradle.properties`が存在しない場合は作成してください

#### このライブラリを利用するbuild.gradle

> repositories

```gd
maven {
    name = "GitHubPackages"
    url = uri("https://maven.pkg.github.com/Logiench/LogienchLib-v2-2")
    credentials {
        // gradle.propertiesから認証情報を読み込む設定
        username = project.findProperty("GITHUB_ACTOR") ?: System.getenv("GITHUB_ACTOR")
        password = project.findProperty("GITHUB_TOKEN") ?: System.getenv("GITHUB_TOKEN")
    }
}
```

> dependencies

```gd
compileOnly("Logiench:logienchlib:2.0.0")
```
