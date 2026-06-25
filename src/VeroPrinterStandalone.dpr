program VeroPrinterStandalone;

uses
  System.StartUpCopy,
  FMX.Forms,
  uMain.View in 'View\uMain.View.pas' {frmMain},
  uVeroPrinter.Service in 'Services\uVeroPrinter.Service.pas';

{$R *.res}

begin
  Application.Initialize;
  Application.CreateForm(TfrmMain, frmMain);
  Application.Run;
end.
