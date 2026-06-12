# Configを追加した際の手順
## ConfigStateを追加した場合
1. di.ConfigModule に作成したクラスを登録し、起動時に確実に読み込まれるようにする
## .ymlなどのファイルを追加した場合
1. ConfigManager の List.of に該当のファイルやフォルダを指定する
2. resourcesにファイルがあることを確認する