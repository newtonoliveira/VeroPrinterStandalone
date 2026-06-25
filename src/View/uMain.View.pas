unit uMain.View;

interface

uses
  System.SysUtils,
  System.Types,
  System.UITypes,
  System.Classes,
  FMX.Types,
  FMX.Controls,
  FMX.Forms,
  FMX.Graphics,
  FMX.Dialogs,
  FMX.StdCtrls,
  FMX.Controls.Presentation,
  FMX.Layouts,
  FMX.Objects,
  FMX.Memo,
  FMX.ScrollBox,
  uVeroPrinter.Service, FMX.Memo.Types;

type
  TfrmMain = class(TForm)
    lytRoot: TLayout;
    rctHeader: TRectangle;
    lblTitulo: TLabel;
    lblSubtitulo: TLabel;
    rctContent: TRectangle;
    lblTexto: TLabel;
    memTexto: TMemo;
    btnImprimir: TButton;
    lblStatusTitulo: TLabel;
    lblStatus: TLabel;
    procedure FormCreate(Sender: TObject);
    procedure btnImprimirClick(Sender: TObject);
  private
    procedure AtualizarStatus(const strMensagem: string);
  end;

var
  frmMain: TfrmMain;

implementation

{$R *.fmx}

procedure TfrmMain.AtualizarStatus(const strMensagem: string);
begin
  lblStatus.Text := strMensagem;
end;

procedure TfrmMain.btnImprimirClick(Sender: TObject);
begin
  try
    AtualizarStatus('Enviando impressao para a Vero...');
    VeroPrinter.ImprimirTexto(memTexto.Lines.Text, True);
    AtualizarStatus('Impressao enviada. Confira o terminal.');
  except
    on E: Exception do
      AtualizarStatus('Erro: ' + E.Message);
  end;
end;

procedure TfrmMain.FormCreate(Sender: TObject);
begin
  memTexto.Lines.Text :=
    'Vero Printer Standalone' + sLineBreak +
    '------------------------' + sLineBreak +
    'Projeto Delphi simples' + sLineBreak +
    'Impressao por imagem' + sLineBreak +
    'SDK Vero validado no N950' + sLineBreak +
    sLineBreak +
    'Digite o texto desejado e toque em IMPRIMIR.';
  AtualizarStatus('Pronto para imprimir.');
end;

end.
