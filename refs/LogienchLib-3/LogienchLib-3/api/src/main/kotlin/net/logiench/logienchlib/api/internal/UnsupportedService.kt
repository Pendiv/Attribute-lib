package net.logiench.logienchlib.api.internal

class UnsupportedService(serviceName: String) :
	UnsupportedOperationException("このプラットフォームでは${serviceName}はサポートされていません")