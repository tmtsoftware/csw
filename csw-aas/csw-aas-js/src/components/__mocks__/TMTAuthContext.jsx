const context = {tmtAuth: {}, isAuthenticated: true}

export const TMTAuthContext = ({
  Consumer(props) {
    return props.children(context)
  }
})
