// ZDesktopManager.cpp : Defines the entry point for the application.
//

#include "ZDesktopManager.h"

class MyFrame : public wxFrame
{
public:
    MyFrame() : wxFrame(NULL, wxID_ANY, "Hello, wxWidgets!")
    {
        // 创建一个静态文本控件并添加到框架
        wxStaticText* text = new wxStaticText(this, wxID_ANY, "Hello, World!\n你好，世界！");

        // 设置文本控件的字体和颜色
        text->SetFont(wxFont(16, wxFONTFAMILY_DEFAULT, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_BOLD));
        text->SetForegroundColour(*wxBLUE);

        // 使用垂直盒子布局管理器来布局文本控件
        wxBoxSizer* sizer = new wxBoxSizer(wxVERTICAL);
        sizer->Add(text, 0, wxALIGN_CENTER | wxTOP, 50);
        SetSizer(sizer);

        // 设置框架的大小
        SetClientSize(400, 300);
    }
};

class MyApp : public wxApp
{
public:
    virtual bool OnInit()
    {
        MyFrame* frame = new MyFrame();
        frame->Show(true);
        return true;
    }
};

// 虽然教程说可以用这个宏定义代替main函数，但我尝试没有成功
// wxIMPLEMENT_APP(MyApp);

int main(int argc, char* argv[])
{
    wxApp::SetInstance(new MyApp);
    return wxEntry(argc, argv);
}