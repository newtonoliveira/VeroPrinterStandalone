unit uVeroPrinter.Service;

interface

uses
  System.SysUtils,
  Androidapi.JNIBridge,
  Androidapi.JNI.App,
  Androidapi.JNI.JavaTypes,
  Androidapi.Helpers,
  Androidapi.Log;

type
  JVeroPrinterRouterClass = interface(JObjectClass)
    ['{8C5AE7C9-4A95-4CA3-8A9D-5A2F4776A4B1}']
    {class} procedure printText(objActivity: JActivity; objText: JString;
      bFeedAuto: Boolean); cdecl;
    {class} procedure printTextAsBitmap(objActivity: JActivity; objText: JString;
      bFeedAuto: Boolean); cdecl;
    {class} procedure printProtocol(objActivity: JActivity; objProtocolJson: JString;
      bFeedAuto: Boolean); cdecl;
  end;

  [JavaSignature('com/veroprinterstandalone/VeroPrinterRouter')]
  JVeroPrinterRouter = interface(JObject)
    ['{BB2F2FD5-18B4-45B4-A6E1-3406DA4E97B7}']
  end;

  TJVeroPrinterRouter = class(TJavaGenericImport<JVeroPrinterRouterClass,
    JVeroPrinterRouter>) end;

  TVeroPrinterService = class
  public
    procedure ImprimirTexto(const strTexto: string; const bFeedAuto: Boolean = True);
  end;

procedure DebugPrinterLog(const strStep, strMessage: string);

var
  VeroPrinter: TVeroPrinterService;

implementation

procedure DebugPrinterLog(const strStep, strMessage: string);
var
  lsLine: AnsiString;
  lTag: MarshaledAString;
begin
  lsLine := AnsiString('[VERO-STANDALONE] ' + strStep + ': ' + strMessage);
  lTag := 'VeroStandalone';
  __android_log_write(ANDROID_LOG_DEBUG, lTag, MarshaledAString(lsLine));
end;

function NormalizaLF(const strTexto: string): string;
begin
  Result := StringReplace(strTexto, #13#10, #10, [rfReplaceAll]);
  Result := StringReplace(Result, #13, #10, [rfReplaceAll]);
end;

procedure TVeroPrinterService.ImprimirTexto(const strTexto: string;
  const bFeedAuto: Boolean);
var
  lsTexto: string;
  lsPreview: string;
begin
  lsTexto := NormalizaLF(Trim(strTexto));
  if lsTexto = '' then
  begin
    DebugPrinterLog('ImprimirTexto', 'texto vazio');
    Exit;
  end;

  lsPreview := Copy(lsTexto, 1, 100);
  lsPreview := StringReplace(lsPreview, #10, '\n', [rfReplaceAll]);
  DebugPrinterLog('ImprimirTexto', 'len=' + Length(lsTexto).ToString + ' preview=' + lsPreview);

  if TAndroidHelper.Activity = nil then
  begin
    DebugPrinterLog('ImprimirTexto', 'Activity=nil');
    Exit;
  end;

  try
    TJVeroPrinterRouter.JavaClass.printTextAsBitmap(
      TAndroidHelper.Activity,
      StringToJString(lsTexto),
      bFeedAuto
    );
    DebugPrinterLog('ImprimirTexto', 'router Java acionado');
  except
    on E: Exception do
      DebugPrinterLog('ImprimirTexto-Exception', E.ClassName + ': ' + E.Message);
  end;
end;

initialization
  VeroPrinter := TVeroPrinterService.Create;

finalization
  VeroPrinter.Free;

end.
